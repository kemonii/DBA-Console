package una.dbadmin.backup;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link OracleCatalog} backed by the {@code DBA_*} views. Names must already be validated and upper-cased.
 */
public class JdbcOracleCatalog implements OracleCatalog {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOracleCatalog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dba_tables WHERE owner = ? AND table_name = ?", Integer.class, schema, table);
        return count != null && count > 0;
    }

    @Override
    public boolean userExists(String user) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dba_users WHERE username = ?", Integer.class, user);
        return count != null && count > 0;
    }
}
