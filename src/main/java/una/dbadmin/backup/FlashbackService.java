package una.dbadmin.backup;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Flashback Table (undo a {@code DROP TABLE}) and Flashback Query ({@code AS OF TIMESTAMP}).
 */
@Service
public class FlashbackService {

    /** Flashback Query is a preview, so the result is capped. */
    private static final int QUERY_LIMIT = 100;

    private final JdbcTemplate jdbcTemplate;

    public FlashbackService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs {@code FLASHBACK TABLE ... TO BEFORE DROP}.
     *
     * @throws IllegalArgumentException if a name is invalid or the table is not in the recycle bin
     */
    public void restoreTable(FlashbackTableRequest request) {
        String schema = OracleIdentifiers.requireValid(request.schema());
        String table = OracleIdentifiers.requireValid(request.table());
        Integer inRecycleBin = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dba_recyclebin WHERE owner = ? AND original_name = ? AND type = 'TABLE'",
                Integer.class, schema, table);
        if (inRecycleBin == null || inRecycleBin == 0) {
            throw new IllegalArgumentException("Table " + schema + "." + table + " is not in the recycle bin");
        }
        jdbcTemplate.execute("FLASHBACK TABLE " + schema + "." + table + " TO BEFORE DROP");
    }

    /**
     * Returns up to {@value #QUERY_LIMIT} rows of the table as they were at the given moment.
     */
    public List<Map<String, Object>> queryAsOf(FlashbackQueryRequest request) {
        String qualified = qualifiedName(request);
        Timestamp at = requireTimestamp(request.timestamp());
        return jdbcTemplate.queryForList(
                "SELECT * FROM " + qualified + " AS OF TIMESTAMP ? FETCH FIRST " + QUERY_LIMIT + " ROWS ONLY", at);
    }

    /**
     * Inserts the rows that existed at the given moment and are missing now.
     *
     * @return number of rows inserted
     */
    public int restoreRows(FlashbackQueryRequest request) {
        String qualified = qualifiedName(request);
        Timestamp at = requireTimestamp(request.timestamp());
        return jdbcTemplate.update("INSERT INTO " + qualified + " SELECT * FROM " + qualified
                + " AS OF TIMESTAMP ? MINUS SELECT * FROM " + qualified, at);
    }

    private static String qualifiedName(FlashbackQueryRequest request) {
        return OracleIdentifiers.requireValid(request.schema()) + "." + OracleIdentifiers.requireValid(request.table());
    }

    private static Timestamp requireTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        return Timestamp.valueOf(timestamp);
    }
}
