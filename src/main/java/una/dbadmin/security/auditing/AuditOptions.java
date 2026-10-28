package una.dbadmin.security.auditing;

import java.util.List;

/**
 * What is being audited.
 *
 * @param statementOptions from {@code DBA_STMT_AUDIT_OPTS}, for example {@code CREATE SESSION} after
 *                         {@code AUDIT SESSION}
 * @param objectOptions    from {@code DBA_OBJ_AUDIT_OPTS}, only objects with INSERT, UPDATE, DELETE or SELECT audited
 */
public record AuditOptions(List<StatementOption> statementOptions, List<ObjectOption> objectOptions) {

    /**
     * A statement audit option.
     *
     * @param userName {@code null} when it applies to every user
     * @param success  {@code BY ACCESS}, {@code BY SESSION} or {@code NOT SET}
     * @param failure  {@code BY ACCESS}, {@code BY SESSION} or {@code NOT SET}
     */
    public record StatementOption(String userName, String option, String success, String failure) {
    }

    /**
     * The audit options of an object. Each value is {@code success/failure}: {@code A} by access, {@code S} by
     * session, {@code -} not audited (for example {@code A/A}).
     */
    public record ObjectOption(String owner, String objectName, String objectType, String insert, String update,
                               String delete, String select) {
    }
}
