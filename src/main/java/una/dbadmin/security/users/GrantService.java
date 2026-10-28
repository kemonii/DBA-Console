package una.dbadmin.security.users;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Grants and revokes privileges and roles with the statements from the course ("Seguridad": {@code GRANT ... TO},
 * {@code REVOKE ... FROM}), and shows what a user or role has.
 *
 * <p>The grantee can be a user, a role or {@code PUBLIC}. Revoking from a user or role maintained by Oracle is
 * refused, so the application cannot strip SYS, SYSTEM or the predefined roles of what they need.
 */
@Service
public class GrantService {

    static final String SYSTEM_PRIVILEGE_NAMES_QUERY = "SELECT name FROM system_privilege_map ORDER BY name";

    static final String SYSTEM_PRIVILEGE_EXISTS_QUERY = "SELECT COUNT(*) FROM system_privilege_map WHERE name = ?";

    static final String SYSTEM_PRIVILEGES_QUERY =
            "SELECT privilege, admin_option FROM dba_sys_privs WHERE grantee = ? ORDER BY privilege";

    static final String ROLES_QUERY = "SELECT granted_role, admin_option, default_role FROM dba_role_privs "
            + "WHERE grantee = ? ORDER BY granted_role";

    static final String MAINTAINED_GRANTEE_QUERY = "SELECT COUNT(*) FROM ("
            + "SELECT username AS name FROM dba_users WHERE oracle_maintained = 'Y' "
            + "UNION ALL SELECT role FROM dba_roles WHERE oracle_maintained = 'Y') WHERE name = ?";

    /** System privilege names are words separated by single spaces, such as {@code CREATE SESSION}. */
    private static final Pattern PRIVILEGE_NAME = Pattern.compile("[A-Z]+( [A-Z]+)*");

    private final JdbcTemplate jdbcTemplate;

    public GrantService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Names in {@code SYSTEM_PRIVILEGE_MAP}, the list of system privileges shown in class. */
    public List<String> listSystemPrivilegeNames() {
        return jdbcTemplate.queryForList(SYSTEM_PRIVILEGE_NAMES_QUERY, String.class);
    }

    public GranteePrivileges privilegesOf(String granteeName) {
        String grantee = OracleIdentifiers.requireValid(granteeName);
        List<GranteePrivileges.SystemPrivilege> system = jdbcTemplate.queryForList(SYSTEM_PRIVILEGES_QUERY, grantee)
                .stream()
                .map(row -> new GranteePrivileges.SystemPrivilege(
                        String.valueOf(row.get("PRIVILEGE")), "YES".equals(row.get("ADMIN_OPTION"))))
                .toList();
        List<GranteePrivileges.Role> roles = jdbcTemplate.queryForList(ROLES_QUERY, grantee).stream()
                .map(row -> new GranteePrivileges.Role(String.valueOf(row.get("GRANTED_ROLE")),
                        "YES".equals(row.get("ADMIN_OPTION")), "YES".equals(row.get("DEFAULT_ROLE"))))
                .toList();
        return new GranteePrivileges(grantee, system, roles);
    }

    /** Runs {@code GRANT <system privilege> TO <grantee> [WITH ADMIN OPTION]}. */
    public void grantSystemPrivilege(String privilegeName, String granteeName, boolean adminOption) {
        String privilege = requireSystemPrivilege(privilegeName);
        String grantee = OracleIdentifiers.requireValid(granteeName);
        jdbcTemplate.execute("GRANT " + privilege + " TO " + grantee + (adminOption ? " WITH ADMIN OPTION" : ""));
    }

    /** Runs {@code REVOKE <system privilege> FROM <grantee>}. */
    public void revokeSystemPrivilege(String privilegeName, String granteeName) {
        String privilege = requireSystemPrivilege(privilegeName);
        String grantee = requireRevocableFrom(granteeName);
        jdbcTemplate.execute("REVOKE " + privilege + " FROM " + grantee);
    }

    /** Runs {@code GRANT <role> TO <grantee> [WITH ADMIN OPTION]}. */
    public void grantRole(String roleName, String granteeName, boolean adminOption) {
        String role = OracleIdentifiers.requireValid(roleName);
        String grantee = OracleIdentifiers.requireValid(granteeName);
        jdbcTemplate.execute("GRANT " + role + " TO " + grantee + (adminOption ? " WITH ADMIN OPTION" : ""));
    }

    /** Runs {@code REVOKE <role> FROM <grantee>}. */
    public void revokeRole(String roleName, String granteeName) {
        String role = OracleIdentifiers.requireValid(roleName);
        String grantee = requireRevocableFrom(granteeName);
        jdbcTemplate.execute("REVOKE " + role + " FROM " + grantee);
    }

    /**
     * System privilege names contain spaces, so they cannot go through {@link OracleIdentifiers}. They are
     * normalized and checked against {@code SYSTEM_PRIVILEGE_MAP} instead, with a bind value.
     */
    private String requireSystemPrivilege(String name) {
        String privilege = name == null ? "" : name.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        Integer count = PRIVILEGE_NAME.matcher(privilege).matches()
                ? jdbcTemplate.queryForObject(SYSTEM_PRIVILEGE_EXISTS_QUERY, Integer.class, privilege)
                : null;
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Unknown system privilege: " + name);
        }
        return privilege;
    }

    private String requireRevocableFrom(String granteeName) {
        String grantee = OracleIdentifiers.requireValid(granteeName);
        Integer maintained = jdbcTemplate.queryForObject(MAINTAINED_GRANTEE_QUERY, Integer.class, grantee);
        if (maintained != null && maintained > 0) {
            throw new IllegalArgumentException(
                    grantee + " is maintained by Oracle; its privileges cannot be revoked here");
        }
        return grantee;
    }
}
