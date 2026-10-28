package una.dbadmin.security.users;

import java.util.List;

/**
 * What a user or role has been granted.
 *
 * @param grantee          user or role name
 * @param systemPrivileges from {@code DBA_SYS_PRIVS}
 * @param roles            from {@code DBA_ROLE_PRIVS}
 * @param objectPrivileges from {@code DBA_TAB_PRIVS}
 */
public record GranteePrivileges(String grantee, List<SystemPrivilege> systemPrivileges, List<Role> roles,
                                List<ObjectPrivilege> objectPrivileges) {

    /** A system privilege, and whether the grantee can grant it to others. */
    public record SystemPrivilege(String privilege, boolean adminOption) {
    }

    /** A granted role, whether the grantee can grant it to others, and whether it is enabled at login. */
    public record Role(String role, boolean adminOption, boolean defaultRole) {
    }

    /** A privilege on an object (table, view, procedure, ...), and whether the grantee can grant it to others. */
    public record ObjectPrivilege(String owner, String objectName, String privilege, boolean grantable) {
    }
}
