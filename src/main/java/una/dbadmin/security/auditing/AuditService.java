package una.dbadmin.security.auditing;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Traditional auditing with the statements and views from the course ("Auditoría Oracle"). The audit trail is
 * written to {@code SYS.AUD$} only while the {@code audit_trail} parameter is {@code DB} (or {@code DB, EXTENDED});
 * changing it needs {@code ALTER SYSTEM ... SCOPE = SPFILE} and a restart, so this service only reads it.
 */
@Service
public class AuditService {

    /** Query from class to check the parameter. */
    static final String PARAMETER_QUERY = "SELECT value FROM v$parameter WHERE name = 'audit_trail'";

    static final String STATEMENT_OPTIONS_QUERY =
            "SELECT user_name, audit_option, success, failure FROM dba_stmt_audit_opts ORDER BY audit_option, user_name";

    /** DBA_OBJ_AUDIT_OPTS has a row for every object, so only the audited ones are read. */
    static final String OBJECT_OPTIONS_QUERY = "SELECT owner, object_name, object_type, ins, upd, del, sel "
            + "FROM dba_obj_audit_opts WHERE ins <> '-/-' OR upd <> '-/-' OR del <> '-/-' OR sel <> '-/-' "
            + "ORDER BY owner, object_name";

    /** Each trail query filters by user only when one is given ({@code ? IS NULL}) and returns the newest first. */
    private static final String FILTER_AND_LIMIT =
            " WHERE (? IS NULL OR username = ?) ORDER BY timestamp DESC FETCH FIRST ? ROWS ONLY";

    static final String TRAIL_QUERY = "SELECT timestamp, username, action_name, owner, obj_name, priv_used, returncode "
            + "FROM dba_audit_trail" + FILTER_AND_LIMIT;

    /** Session query from class, with its DECODE of the return code. */
    static final String SESSIONS_QUERY = "SELECT timestamp, logoff_time, os_username, username, terminal, "
            + "DECODE(returncode, 0, 'Connected', 1005, 'Failed - null password', 1017, 'Failed', returncode) "
            + "AS outcome FROM dba_audit_session" + FILTER_AND_LIMIT;

    /** Object query from class, with its DECODE of the return code. */
    static final String OBJECTS_QUERY = "SELECT timestamp, os_username, username, terminal, owner, obj_name, "
            + "action_name, DECODE(returncode, 0, 'Done', returncode) AS outcome FROM dba_audit_object"
            + FILTER_AND_LIMIT;

    static final int DEFAULT_LIMIT = 100;
    static final int MAX_LIMIT = 1000;

    /** The table actions audited in class; DBA_OBJ_AUDIT_OPTS shows them as INS, UPD, DEL and SEL. */
    static final Set<String> TABLE_ACTIONS = Set.of("INSERT", "UPDATE", "DELETE", "SELECT");

    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** The value of {@code audit_trail}, and whether it writes to the database audit trail that this module reads. */
    public AuditTrailParameter auditTrail() {
        List<String> values = jdbcTemplate.queryForList(PARAMETER_QUERY, String.class);
        String value = values.isEmpty() || values.get(0) == null ? "NONE" : values.get(0);
        return new AuditTrailParameter(value, value.toUpperCase(Locale.ROOT).startsWith("DB"));
    }

    public AuditOptions options() {
        List<AuditOptions.StatementOption> statements = jdbcTemplate.queryForList(STATEMENT_OPTIONS_QUERY).stream()
                .map(row -> new AuditOptions.StatementOption(text(row, "USER_NAME"), text(row, "AUDIT_OPTION"),
                        text(row, "SUCCESS"), text(row, "FAILURE")))
                .toList();
        List<AuditOptions.ObjectOption> objects = jdbcTemplate.queryForList(OBJECT_OPTIONS_QUERY).stream()
                .map(row -> new AuditOptions.ObjectOption(text(row, "OWNER"), text(row, "OBJECT_NAME"),
                        text(row, "OBJECT_TYPE"), text(row, "INS"), text(row, "UPD"), text(row, "DEL"),
                        text(row, "SEL")))
                .toList();
        return new AuditOptions(statements, objects);
    }

    /** Runs {@code AUDIT SESSION [WHENEVER [NOT] SUCCESSFUL]}: records logins in DBA_AUDIT_SESSION. */
    public void auditSessions(Whenever whenever) {
        jdbcTemplate.execute("AUDIT SESSION" + orAlways(whenever).clause());
    }

    /** Runs {@code NOAUDIT SESSION [WHENEVER [NOT] SUCCESSFUL]}. */
    public void stopAuditingSessions(Whenever whenever) {
        jdbcTemplate.execute("NOAUDIT SESSION" + orAlways(whenever).clause());
    }

    /**
     * Runs {@code AUDIT <actions> ON <owner>.<table> BY ACCESS | BY SESSION}, for example
     * {@code AUDIT INSERT, UPDATE, DELETE ON HR.JOBS BY ACCESS}.
     *
     * @param byAccess one record per statement ({@code BY ACCESS}) instead of one per session
     */
    public void auditTable(String owner, String table, List<String> actions, boolean byAccess) {
        jdbcTemplate.execute("AUDIT " + requireActions(actions) + " ON " + qualified(owner, table)
                + (byAccess ? " BY ACCESS" : " BY SESSION"));
    }

    /** Runs {@code NOAUDIT <actions> ON <owner>.<table>}. */
    public void stopAuditingTable(String owner, String table, List<String> actions) {
        jdbcTemplate.execute("NOAUDIT " + requireActions(actions) + " ON " + qualified(owner, table));
    }

    /**
     * Newest rows of {@code DBA_AUDIT_TRAIL}.
     *
     * @param username only this user's actions, or every user when {@code null} or blank
     * @param limit    number of rows, {@value #DEFAULT_LIMIT} when {@code null}, at most {@value #MAX_LIMIT}
     */
    public List<AuditEntries.TrailEntry> trail(String username, Integer limit) {
        return query(TRAIL_QUERY, username, limit).stream()
                .map(row -> new AuditEntries.TrailEntry(time(row, "TIMESTAMP"), text(row, "USERNAME"),
                        text(row, "ACTION_NAME"), text(row, "OWNER"), text(row, "OBJ_NAME"), text(row, "PRIV_USED"),
                        row.get("RETURNCODE") instanceof Number code ? code.intValue() : 0))
                .toList();
    }

    /** Newest rows of {@code DBA_AUDIT_SESSION}; same parameters as {@link #trail}. */
    public List<AuditEntries.SessionEntry> sessions(String username, Integer limit) {
        return query(SESSIONS_QUERY, username, limit).stream()
                .map(row -> new AuditEntries.SessionEntry(time(row, "TIMESTAMP"), time(row, "LOGOFF_TIME"),
                        text(row, "OS_USERNAME"), text(row, "USERNAME"), text(row, "TERMINAL"),
                        text(row, "OUTCOME")))
                .toList();
    }

    /** Newest rows of {@code DBA_AUDIT_OBJECT}; same parameters as {@link #trail}. */
    public List<AuditEntries.ObjectEntry> objects(String username, Integer limit) {
        return query(OBJECTS_QUERY, username, limit).stream()
                .map(row -> new AuditEntries.ObjectEntry(time(row, "TIMESTAMP"), text(row, "OS_USERNAME"),
                        text(row, "USERNAME"), text(row, "TERMINAL"), text(row, "OWNER"), text(row, "OBJ_NAME"),
                        text(row, "ACTION_NAME"), text(row, "OUTCOME")))
                .toList();
    }

    private List<Map<String, Object>> query(String sql, String username, Integer limit) {
        String user = username == null || username.isBlank() ? null : OracleIdentifiers.requireValid(username);
        int rows = limit == null ? DEFAULT_LIMIT : limit;
        if (rows < 1 || rows > MAX_LIMIT) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_LIMIT);
        }
        return jdbcTemplate.queryForList(sql, user, user, rows);
    }

    private static LocalDateTime time(Map<String, Object> row, String column) {
        return row.get(column) instanceof Timestamp value ? value.toLocalDateTime() : null;
    }

    private static String qualified(String owner, String table) {
        return OracleIdentifiers.requireValid(owner) + "." + OracleIdentifiers.requireValid(table);
    }

    private static String requireActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("Select at least one action to audit");
        }
        List<String> valid = actions.stream()
                .map(action -> action == null ? "" : action.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        for (String action : valid) {
            if (!TABLE_ACTIONS.contains(action)) {
                throw new IllegalArgumentException("Unknown table action: " + action);
            }
        }
        return String.join(", ", valid);
    }

    private static Whenever orAlways(Whenever whenever) {
        return whenever == null ? Whenever.ALWAYS : whenever;
    }

    private static String text(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }
}
