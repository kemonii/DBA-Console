package una.dbadmin.security.users;

/**
 * A role, as listed in {@code DBA_ROLES}.
 *
 * @param name             role name
 * @param oracleMaintained whether Oracle created it (CONNECT, RESOURCE, DBA, ...); those can be granted but not dropped
 */
public record DbRole(String name, boolean oracleMaintained) {
}
