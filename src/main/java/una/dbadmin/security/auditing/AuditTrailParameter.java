package una.dbadmin.security.auditing;

/**
 * The {@code audit_trail} initialization parameter.
 *
 * @param value   as shown by {@code V$PARAMETER}, for example {@code DB}, {@code DB, EXTENDED}, {@code OS} or
 *                {@code NONE}
 * @param enabled whether audit records go to {@code SYS.AUD$}, which is what {@code DBA_AUDIT_TRAIL} reads
 */
public record AuditTrailParameter(String value, boolean enabled) {
}
