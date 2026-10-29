package una.dbadmin.security.users;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Creates, locks, unlocks and drops database users, and assigns their profile, with the statements from the course ("Seguridad"). Users created by Oracle
 * itself ({@code ORACLE_MAINTAINED = 'Y'}, such as SYS and SYSTEM) are neither listed nor changed.
 */
@Service
public class UserService {

    static final String LIST_QUERY = "SELECT username, account_status, default_tablespace, temporary_tablespace, "
            + "profile, created FROM dba_users WHERE oracle_maintained = 'N' ORDER BY username";

    static final String MAINTAINED_QUERY = "SELECT oracle_maintained FROM dba_users WHERE username = ?";

    static final int MAX_PASSWORD_LENGTH = 128;

    /** One terabyte; larger quotas are better expressed as UNLIMITED. */
    static final int MAX_QUOTA_MB = 1_048_576;

    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DbUser> list() {
        return jdbcTemplate.queryForList(LIST_QUERY).stream()
                .map(row -> new DbUser(
                        String.valueOf(row.get("USERNAME")),
                        String.valueOf(row.get("ACCOUNT_STATUS")),
                        String.valueOf(row.get("DEFAULT_TABLESPACE")),
                        String.valueOf(row.get("TEMPORARY_TABLESPACE")),
                        String.valueOf(row.get("PROFILE")),
                        row.get("CREATED") instanceof Timestamp created ? created.toLocalDateTime() : null))
                .toList();
    }

    /**
     * Runs {@code CREATE USER ... IDENTIFIED BY "..." [DEFAULT TABLESPACE ...] [TEMPORARY TABLESPACE ...]
     * [QUOTA ... ON ...]}. A password cannot be a bind parameter, so it is validated and written in double quotes.
     *
     * @param quotaMb        quota on the default tablespace, or {@code null} for none
     * @param unlimitedQuota {@code QUOTA UNLIMITED} on the default tablespace; takes precedence over {@code quotaMb}
     * @return the user name as Oracle stores it (upper case)
     */
    public String create(String username, String password, String defaultTablespace, String temporaryTablespace,
                         Integer quotaMb, boolean unlimitedQuota) {
        String user = OracleIdentifiers.requireValid(username);
        StringBuilder sql = new StringBuilder("CREATE USER ").append(user)
                .append(" IDENTIFIED BY ").append(quotedPassword(password));
        String tablespace = blank(defaultTablespace) ? null : OracleIdentifiers.requireValid(defaultTablespace);
        if (tablespace != null) {
            sql.append(" DEFAULT TABLESPACE ").append(tablespace);
        }
        if (!blank(temporaryTablespace)) {
            sql.append(" TEMPORARY TABLESPACE ").append(OracleIdentifiers.requireValid(temporaryTablespace));
        }
        if (unlimitedQuota || quotaMb != null) {
            if (tablespace == null) {
                throw new IllegalArgumentException("Una cuota necesita un tablespace por defecto");
            }
            sql.append(" QUOTA ").append(unlimitedQuota ? "UNLIMITED" : requireQuota(quotaMb) + "M")
                    .append(" ON ").append(tablespace);
        }
        jdbcTemplate.execute(sql.toString());
        return user;
    }

    /** Runs {@code ALTER USER ... ACCOUNT LOCK} or {@code ACCOUNT UNLOCK}. */
    public void setLocked(String username, boolean locked) {
        String user = requireManageable(username);
        jdbcTemplate.execute("ALTER USER " + user + " ACCOUNT " + (locked ? "LOCK" : "UNLOCK"));
    }

    /** Runs {@code ALTER USER ... PROFILE ...}; a user has exactly one profile. */
    public void setProfile(String username, String profileName) {
        String user = requireManageable(username);
        String profile = OracleIdentifiers.requireValid(profileName);
        jdbcTemplate.execute("ALTER USER " + user + " PROFILE " + profile);
    }

    /**
     * Runs {@code DROP USER ... [CASCADE]}. Without {@code CASCADE}, Oracle refuses to drop a user that owns objects.
     *
     * @throws IllegalArgumentException without {@code confirm}, or for a user maintained by Oracle
     */
    public void drop(String username, boolean cascade, boolean confirm) {
        String user = requireManageable(username);
        if (!confirm) {
            throw new IllegalArgumentException("Eliminar un usuario no se puede deshacer. Envíe confirm=true para eliminarlo");
        }
        jdbcTemplate.execute("DROP USER " + user + (cascade ? " CASCADE" : ""));
    }

    /**
     * Returns the validated user name after checking that the user exists and was not created by Oracle.
     */
    String requireManageable(String username) {
        String user = OracleIdentifiers.requireValid(username);
        List<String> maintained = jdbcTemplate.queryForList(MAINTAINED_QUERY, String.class, user);
        if (maintained.isEmpty()) {
            throw new IllegalArgumentException("El usuario " + user + " no existe");
        }
        if ("Y".equals(maintained.get(0))) {
            throw new IllegalArgumentException("El usuario " + user + " es mantenido por Oracle y no se puede modificar aquí");
        }
        return user;
    }

    static String quotedPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña es demasiado larga");
        }
        if (password.indexOf('"') >= 0 || password.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("La contraseña no puede contener comillas dobles ni caracteres de control");
        }
        return "\"" + password + "\"";
    }

    private static int requireQuota(int megabytes) {
        if (megabytes < 1 || megabytes > MAX_QUOTA_MB) {
            throw new IllegalArgumentException("La cuota debe estar entre 1 y " + MAX_QUOTA_MB + " MB");
        }
        return megabytes;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
