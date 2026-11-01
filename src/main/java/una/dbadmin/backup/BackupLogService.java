package una.dbadmin.backup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Reads the {@code .log} files that {@code expdp} and {@code impdp} write into an Oracle directory object. The
 * application must run on a host that can read the directory path, as it already must to launch Data Pump.
 */
@Service
public class BackupLogService {

    private static final String LOG_EXTENSION = ".log";

    /** Data Pump logs can be long; the end of the file is where the outcome is. */
    private static final int MAX_LINES = 500;

    private final JdbcTemplate jdbcTemplate;

    public BackupLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param directory Oracle directory object name
     * @param fileName  log file name such as {@code emp.log} or {@code NW.LOG}; no path, only the identifier-style name
     *                  plus {@code .log} in any case
     * @return up to the last {@value #MAX_LINES} lines of the file
     * @throws IllegalArgumentException if a name is invalid, the directory does not exist or the file is not there
     */
    public List<String> read(String directory, String fileName) {
        String validFileName = requireLogFileName(fileName);
        String directoryName = OracleIdentifiers.requireValid(directory);

        List<String> paths = jdbcTemplate.queryForList(
                "SELECT directory_path FROM dba_directories WHERE directory_name = ?", String.class, directoryName);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("El directorio " + directoryName + " no existe");
        }

        Path base = Path.of(paths.get(0)).toAbsolutePath().normalize();
        Path file = base.resolve(validFileName).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException(
                    "No se encontró el archivo de log " + validFileName + " en el directorio " + directoryName);
        }
        return tail(readLines(file));
    }

    /** The extension is compared in any case: logs created outside the application may end in {@code .LOG}. */
    private static String requireLogFileName(String fileName) {
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(LOG_EXTENSION)) {
            throw new IllegalArgumentException("Solo se pueden leer archivos " + LOG_EXTENSION);
        }
        OracleIdentifiers.requireValid(fileName.substring(0, fileName.length() - LOG_EXTENSION.length()));
        return fileName;
    }

    private static List<String> readLines(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + file.getFileName(), e);
        }
    }

    private static List<String> tail(List<String> lines) {
        return lines.size() <= MAX_LINES ? lines : lines.subList(lines.size() - MAX_LINES, lines.size());
    }
}
