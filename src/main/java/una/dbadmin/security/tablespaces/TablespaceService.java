package una.dbadmin.security.tablespaces;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Reads tablespace usage and datafiles with the queries from the course ("Mantenimiento básico de bases de
 * datos", class 4).
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

    private static final double BYTES_PER_MB = 1024.0 * 1024.0;

    private final JdbcTemplate jdbcTemplate;

    public TablespaceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    private static double number(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value instanceof Number n ? n.doubleValue() : 0;
    }

    private static double megabytes(Map<String, Object> row, String column) {
        return Math.round(number(row, column) / BYTES_PER_MB * 100) / 100.0;
    }
}
