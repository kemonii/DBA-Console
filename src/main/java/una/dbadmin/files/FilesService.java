package una.dbadmin.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Manages Oracle directory objects and the backup files stored in their paths.
 *
 * <p>Files are read with Java I/O, so the application must run on the database host. Every file name is validated
 * by {@link BackupFileNames} and every resolved path is checked to stay inside the directory's real path.
 */
@Service
public class FilesService {

    /** Maximum number of lines returned when viewing a log. */
    static final int MAX_LOG_LINES = 1000;

    /** Maximum number of characters returned when viewing a log. */
    static final int MAX_LOG_CHARS = 256 * 1024;

    private static final int MAX_PATH_LENGTH = 4000;

    private final JdbcTemplate jdbcTemplate;

    public FilesService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---- directory objects -------------------------------------------------------------------------------------

    public List<DirectoryObject> listDirectories() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT directory_name, directory_path FROM dba_directories ORDER BY directory_name");
        return rows.stream()
                .map(row -> new DirectoryObject(
                        String.valueOf(row.get("DIRECTORY_NAME")), String.valueOf(row.get("DIRECTORY_PATH"))))
                .toList();
    }

    /**
     * Creates or replaces a directory object.
     *
     * @return the directory name as Oracle stores it (upper case)
     */
    public String createDirectory(String name, String path) {
        String directory = OracleIdentifiers.requireValid(name);
        String safePath = requireSafePath(path);
        jdbcTemplate.execute("CREATE OR REPLACE DIRECTORY " + directory + " AS '" + safePath + "'");
        return directory;
    }

    public void grantAccess(String directoryName, String user) {
        String directory = OracleIdentifiers.requireValid(directoryName);
        String grantee = OracleIdentifiers.requireValid(user);
        jdbcTemplate.execute("GRANT READ, WRITE ON DIRECTORY " + directory + " TO " + grantee);
    }

    public void revokeAccess(String directoryName, String user) {
        String directory = OracleIdentifiers.requireValid(directoryName);
        String grantee = OracleIdentifiers.requireValid(user);
        jdbcTemplate.execute("REVOKE READ, WRITE ON DIRECTORY " + directory + " FROM " + grantee);
    }

    public void dropDirectory(String name) {
        jdbcTemplate.execute("DROP DIRECTORY " + OracleIdentifiers.requireValid(name));
    }

    // ---- backup files ------------------------------------------------------------------------------------------

    /** Lists the {@code .dmp} and {@code .log} files of the directory, sorted by name. */
    public List<BackupFile> listFiles(String directoryName) {
        Path base = resolveDirectory(directoryName);
        try (Stream<Path> entries = Files.list(base)) {
            return entries
                    .filter(entry -> BackupFileNames.isValid(entry.getFileName().toString()))
                    .filter(entry -> isRegularFileInside(base, entry))
                    .map(FilesService::describe)
                    .sorted(Comparator.comparing(BackupFile::name))
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Returns the beginning of a {@code .log} file, limited to {@link #MAX_LOG_LINES} lines and {@link #MAX_LOG_CHARS} characters. */
    public LogContent readLog(String directoryName, String fileName) {
        if (!BackupFileNames.isLog(BackupFileNames.requireValid(fileName))) {
            throw new IllegalArgumentException("Solo se pueden ver archivos .log");
        }
        Path file = resolveFile(resolveDirectory(directoryName), fileName);
        StringBuilder text = new StringBuilder();
        int completedLines = 0;
        boolean truncated = false;
        // InputStreamReader replaces malformed bytes instead of failing, which suits logs of unknown encoding.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                if (text.length() >= MAX_LOG_CHARS || completedLines >= MAX_LOG_LINES) {
                    truncated = true;
                    break;
                }
                text.append((char) c);
                if (c == '\n') {
                    completedLines++;
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new LogContent(fileName, text.toString(), truncated);
    }

    /**
     * Deletes a backup file.
     *
     * @param confirm must be {@code true}; deleting is irreversible, so the caller has to ask for it explicitly
     */
    public void deleteFile(String directoryName, String fileName, boolean confirm) {
        if (!confirm) {
            throw new IllegalArgumentException("Eliminar un archivo requiere confirmación (confirm=true)");
        }
        BackupFileNames.requireValid(fileName);
        Path file = resolveFile(resolveDirectory(directoryName), fileName);
        try {
            Files.delete(file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    /** Looks up the directory object's path with a bind parameter and resolves it to a real directory. */
    private Path resolveDirectory(String directoryName) {
        String directory = OracleIdentifiers.requireValid(directoryName);
        List<String> paths = jdbcTemplate.queryForList(
                "SELECT directory_path FROM dba_directories WHERE directory_name = ?", String.class, directory);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el objeto directorio: " + directory);
        }
        try {
            Path base = Path.of(paths.get(0)).toRealPath();
            if (!Files.isDirectory(base)) {
                throw new IllegalArgumentException("La ruta del directorio no es un directorio: " + paths.get(0));
            }
            return base;
        } catch (NoSuchFileException | InvalidPathException ex) {
            throw new IllegalArgumentException(
                    "La ruta del directorio no existe en este servidor: " + paths.get(0));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Resolves a validated file name inside {@code base} and returns its real path, rejecting anything that is not
     * a regular file located directly in the directory (including symbolic links that point elsewhere).
     */
    private static Path resolveFile(Path base, String fileName) {
        Path candidate = base.resolve(BackupFileNames.requireValid(fileName)).normalize();
        if (!base.equals(candidate.getParent())) {
            throw new IllegalArgumentException("El nombre del archivo está fuera del directorio");
        }
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("No se encontró el archivo: " + fileName);
        }
        try {
            Path real = candidate.toRealPath();
            if (!real.startsWith(base)) {
                throw new IllegalArgumentException("El nombre del archivo está fuera del directorio");
            }
            if (!Files.isRegularFile(real)) {
                throw new IllegalArgumentException("No es un archivo regular: " + fileName);
            }
            return candidate;
        } catch (NoSuchFileException ex) {
            throw new IllegalArgumentException("No se encontró el archivo: " + fileName);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static boolean isRegularFileInside(Path base, Path entry) {
        try {
            Path real = entry.toRealPath();
            return real.startsWith(base) && Files.isRegularFile(real);
        } catch (IOException ex) {
            return false;
        }
    }

    private static BackupFile describe(Path file) {
        try {
            return new BackupFile(
                    file.getFileName().toString(), Files.size(file), Files.getLastModifiedTime(file).toInstant());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String requireSafePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("La ruta del directorio es obligatoria");
        }
        if (path.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("La ruta del directorio es demasiado larga");
        }
        if (path.indexOf('\'') >= 0 || path.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("La ruta del directorio contiene caracteres no válidos");
        }
        return path;
    }
}
