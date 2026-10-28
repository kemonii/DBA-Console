package una.dbadmin.security.auditing;

import java.time.LocalDateTime;

/**
 * Rows of the audit trail views queried in class.
 */
public final class AuditEntries {

    private AuditEntries() {
    }

    /** A row of {@code DBA_AUDIT_TRAIL}: every audited action. */
    public record TrailEntry(LocalDateTime timestamp, String username, String actionName, String owner,
                             String objectName, String privilegeUsed, int returnCode) {
    }

    /**
     * A row of {@code DBA_AUDIT_SESSION}: a login attempt.
     *
     * @param outcome {@code Connected}, {@code Failed} (ORA-01017), {@code Failed - null password} (ORA-01005) or the
     *                Oracle error code, as decoded in class
     */
    public record SessionEntry(LocalDateTime timestamp, LocalDateTime logoffTime, String osUsername, String username,
                               String terminal, String outcome) {
    }

    /**
     * A row of {@code DBA_AUDIT_OBJECT}: an audited action on an object.
     *
     * @param outcome {@code Done}, or the Oracle error code when the statement failed
     */
    public record ObjectEntry(LocalDateTime timestamp, String osUsername, String username, String terminal,
                              String owner, String objectName, String actionName, String outcome) {
    }
}
