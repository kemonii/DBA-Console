package una.dbadmin.security.tablespaces;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Creates, resizes and drops tablespaces with the statements from the course ("Mantenimiento básico de bases de
 * datos", class 4). New datafiles are always created under the configured {@code DATAFILE_DIR}, so a request only
 * names the file, never a path.
 */
@Service
public class TablespaceService {

    /** Space usage script from class 4: allocated size from DBA_DATA_FILES, free space from DBA_FREE_SPACE. */
    static final String USAGE_QUERY = """
            SELECT tablespace_name,
                   ROUND(SUM(total_bytes) / 1024 / 1024, 2) AS total_mb,
                   ROUND(SUM(total_bytes - free_bytes) / 1024 / 1024, 2) AS used_mb,
                   ROUND(SUM(free_bytes) / 1024 / 1024, 2) AS free_mb,
                   ROUND(SUM(total_bytes - free_bytes) * 100 / SUM(total_bytes), 2) AS used_percent,
                   ROUND(SUM(free_bytes) * 100 / SUM(total_bytes), 2) AS free_percent
              FROM (SELECT tablespace_name, bytes AS total_bytes, 0 AS free_bytes FROM dba_data_files
                    UNION ALL
                    SELECT tablespace_name, 0 AS total_bytes, bytes AS free_bytes FROM dba_free_space)
             GROUP BY tablespace_name
             ORDER BY tablespace_name""";

    static final String DATAFILES_QUERY = "SELECT file_id, file_name, bytes, autoextensible, maxbytes "
            + "FROM dba_data_files WHERE tablespace_name = ? ORDER BY file_id";

    static final String DATAFILE_PATH_QUERY =
            "SELECT file_name FROM dba_data_files WHERE file_id = ? AND tablespace_name = ?";

    /** Largest smallfile datafile with the default 8 KB block size (4,194,303 blocks), rounded down. */
    static final int MAX_SIZE_MB = 32767;

    /** Dropping these breaks the database, so the request is refused before Oracle is asked. */
    private static final Set<String> PROTECTED = Set.of("SYSTEM", "SYSAUX");

    /** A plain file name ending in .dbf: no directory separators, so it always lands in DATAFILE_DIR. */
    private static final Pattern DATAFILE_NAME =
            Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,95}\\.dbf", Pattern.CASE_INSENSITIVE);

    private static final double BYTES_PER_MB = 1024.0 * 1024.0;

    private final JdbcTemplate jdbcTemplate;
    private final String datafileDir;

    public TablespaceService(JdbcTemplate jdbcTemplate, @Value("${app.oracle.datafile-dir}") String datafileDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.datafileDir = datafileDir;
    }

    public List<TablespaceUsage> listUsage() {
        return jdbcTemplate.queryForList(USAGE_QUERY).stream()
                .map(row -> new TablespaceUsage(
                        String.valueOf(row.get("TABLESPACE_NAME")),
                        number(row, "TOTAL_MB"),
                        number(row, "USED_MB"),
                        number(row, "FREE_MB"),
                        number(row, "USED_PERCENT"),
                        number(row, "FREE_PERCENT")))
                .toList();
    }

    public List<Datafile> listDatafiles(String tablespaceName) {
        String tablespace = OracleIdentifiers.requireValid(tablespaceName);
        return jdbcTemplate.queryForList(DATAFILES_QUERY, tablespace).stream()
                .map(row -> new Datafile(
                        (int) number(row, "FILE_ID"),
                        String.valueOf(row.get("FILE_NAME")),
                        megabytes(row, "BYTES"),
                        "YES".equals(row.get("AUTOEXTENSIBLE")),
                        megabytes(row, "MAXBYTES")))
                .toList();
    }

    /**
     * Runs {@code CREATE TABLESPACE ... DATAFILE ... SIZE ... AUTOEXTEND ON NEXT ... MAXSIZE ...}.
     *
     * @param maxSizeMb {@code null} for {@code MAXSIZE UNLIMITED}
     * @return the tablespace name as Oracle stores it (upper case)
     */
    public String create(String name, String fileName, Integer sizeMb, Integer nextMb, Integer maxSizeMb) {
        String tablespace = OracleIdentifiers.requireValid(name);
        String path = datafilePath(fileName);
        int size = requireSize(sizeMb, "Initial size");
        int next = requireSize(nextMb, "Autoextend increment");
        String maxSize = "UNLIMITED";
        if (maxSizeMb != null) {
            int max = requireSize(maxSizeMb, "Maximum size");
            if (max < size) {
                throw new IllegalArgumentException("Maximum size must be at least the initial size");
            }
            maxSize = max + "M";
        }
        jdbcTemplate.execute("CREATE TABLESPACE " + tablespace + " DATAFILE '" + path + "' SIZE " + size
                + "M AUTOEXTEND ON NEXT " + next + "M MAXSIZE " + maxSize);
        return tablespace;
    }

    /** Runs {@code ALTER TABLESPACE ... ADD DATAFILE ... SIZE ...}. */
    public void addDatafile(String tablespaceName, String fileName, Integer sizeMb) {
        String tablespace = OracleIdentifiers.requireValid(tablespaceName);
        String path = datafilePath(fileName);
        int size = requireSize(sizeMb, "Size");
        jdbcTemplate.execute("ALTER TABLESPACE " + tablespace + " ADD DATAFILE '" + path + "' SIZE " + size + "M");
    }

    /**
     * Runs {@code ALTER DATABASE DATAFILE ... RESIZE ...}. The file is chosen by number and must belong to the
     * tablespace; its path is read from the data dictionary.
     *
     * @throws IllegalArgumentException if the file is not a datafile of the tablespace
     */
    public void resizeDatafile(String tablespaceName, int fileId, Integer sizeMb) {
        String tablespace = OracleIdentifiers.requireValid(tablespaceName);
        int size = requireSize(sizeMb, "New size");
        List<String> paths = jdbcTemplate.queryForList(DATAFILE_PATH_QUERY, String.class, fileId, tablespace);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Datafile " + fileId + " does not belong to tablespace " + tablespace);
        }
        String path = requireQuotable(paths.get(0));
        jdbcTemplate.execute("ALTER DATABASE DATAFILE '" + path + "' RESIZE " + size + "M");
    }

    /**
     * Runs {@code DROP TABLESPACE ... INCLUDING CONTENTS AND DATAFILES}, which removes every object in it and its
     * files on disk.
     *
     * @throws IllegalArgumentException without {@code confirm}, or for SYSTEM and SYSAUX
     */
    public void drop(String name, boolean confirm) {
        String tablespace = OracleIdentifiers.requireValid(name);
        if (PROTECTED.contains(tablespace)) {
            throw new IllegalArgumentException("Tablespace " + tablespace + " cannot be dropped");
        }
        if (!confirm) {
            throw new IllegalArgumentException("Dropping a tablespace deletes its objects and datafiles. "
                    + "Send confirm=true to drop it");
        }
        jdbcTemplate.execute("DROP TABLESPACE " + tablespace + " INCLUDING CONTENTS AND DATAFILES");
    }

    private String datafilePath(String fileName) {
        if (fileName == null || !DATAFILE_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException(
                    "Datafile name must be a plain file name ending in .dbf, for example ventas01.dbf");
        }
        if (datafileDir == null || datafileDir.isBlank()) {
            throw new IllegalStateException("DATAFILE_DIR is not configured");
        }
        try {
            return requireQuotable(Path.of(datafileDir).resolve(fileName).toString());
        } catch (InvalidPathException ex) {
            throw new IllegalStateException("DATAFILE_DIR is not a valid path: " + datafileDir, ex);
        }
    }

    private static String requireQuotable(String path) {
        if (path.indexOf('\'') >= 0 || path.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Datafile path contains invalid characters");
        }
        return path;
    }

    private static int requireSize(Integer megabytes, String label) {
        if (megabytes == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (megabytes < 1 || megabytes > MAX_SIZE_MB) {
            throw new IllegalArgumentException(label + " must be between 1 and " + MAX_SIZE_MB + " MB");
        }
        return megabytes;
    }

    private static double number(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Number n ? n.doubleValue() : 0;
    }

    private static double megabytes(Map<String, Object> row, String column) {
        return Math.round(number(row, column) / BYTES_PER_MB * 100) / 100.0;
    }
}
