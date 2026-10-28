package una.dbadmin.security.auditing;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    private static String text(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }
}
