package una.dbadmin.security.users;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Creates and drops roles with the statements from the course ("Seguridad": a role groups privileges to grant them
 * to users).
 */
@Service
public class RoleService {

    /** Roles created by administrators first, then the ones maintained by Oracle. */
    static final String LIST_QUERY = "SELECT role, oracle_maintained FROM dba_roles ORDER BY oracle_maintained, role";

    static final String MAINTAINED_QUERY = "SELECT oracle_maintained FROM dba_roles WHERE role = ?";

    private final JdbcTemplate jdbcTemplate;

    public RoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DbRole> list() {
        return jdbcTemplate.queryForList(LIST_QUERY).stream()
                .map(row -> new DbRole(String.valueOf(row.get("ROLE")), "Y".equals(row.get("ORACLE_MAINTAINED"))))
                .toList();
    }

    /**
     * Runs {@code CREATE ROLE ...} (a role without password, {@code NOT IDENTIFIED}).
     *
     * @return the role name as Oracle stores it (upper case)
     */
    public String create(String name) {
        String role = OracleIdentifiers.requireValid(name);
        jdbcTemplate.execute("CREATE ROLE " + role);
        return role;
    }

    /**
     * Runs {@code DROP ROLE ...}. Users and roles that had it lose its privileges.
     *
     * @throws IllegalArgumentException if the role does not exist or is maintained by Oracle
     */
    public void drop(String name) {
        String role = OracleIdentifiers.requireValid(name);
        List<String> maintained = jdbcTemplate.queryForList(MAINTAINED_QUERY, String.class, role);
        if (maintained.isEmpty()) {
            throw new IllegalArgumentException("Role " + role + " does not exist");
        }
        if ("Y".equals(maintained.get(0))) {
            throw new IllegalArgumentException("Role " + role + " is maintained by Oracle and cannot be dropped");
        }
        jdbcTemplate.execute("DROP ROLE " + role);
    }
}
