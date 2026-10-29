package una.dbadmin.security.users;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Profiles with the statements from the course ("Seguridad": a profile limits the resources and passwords of the
 * users that have it).
 */
@Service
public class ProfileService {

    static final String DEFAULT = "DEFAULT";

    static final String LIST_QUERY = "SELECT profile, resource_name, resource_type, limit FROM dba_profiles "
            + "ORDER BY profile, resource_type, resource_name";

    /**
     * The limits from the class example that take a whole number: kernel limits (sessions, CPU in hundredths of a
     * second, minutes, blocks) and password limits (attempts and days). PRIVATE_SGA, COMPOSITE_LIMIT and
     * PASSWORD_VERIFY_FUNCTION take other kinds of values and are left out. The order is the one used in
     * statements.
     */
    static final List<String> RESOURCES = List.of(
            "SESSIONS_PER_USER", "CPU_PER_SESSION", "CPU_PER_CALL", "CONNECT_TIME", "IDLE_TIME",
            "LOGICAL_READS_PER_SESSION", "LOGICAL_READS_PER_CALL",
            "FAILED_LOGIN_ATTEMPTS", "PASSWORD_LIFE_TIME", "PASSWORD_REUSE_TIME", "PASSWORD_REUSE_MAX",
            "PASSWORD_LOCK_TIME", "PASSWORD_GRACE_TIME");

    private static final Pattern WHOLE_NUMBER = Pattern.compile("[1-9]\\d{0,8}");

    private final JdbcTemplate jdbcTemplate;

    public ProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs {@code CREATE PROFILE ... LIMIT <resource> <value> ...}, as in the class example.
     *
     * @param limits resource name to a whole number, {@code UNLIMITED} or {@code DEFAULT}; at least one
     * @return the profile name as Oracle stores it (upper case)
     */
    public String create(String name, Map<String, String> limits) {
        String profile = OracleIdentifiers.requireValid(name);
        jdbcTemplate.execute("CREATE PROFILE " + profile + " LIMIT " + limitClause(limits));
        return profile;
    }

    /** Runs {@code ALTER PROFILE ... LIMIT ...}, for example {@code ALTER PROFILE DEFAULT LIMIT IDLE_TIME 20}. */
    public void alter(String name, Map<String, String> limits) {
        String profile = OracleIdentifiers.requireValid(name);
        jdbcTemplate.execute("ALTER PROFILE " + profile + " LIMIT " + limitClause(limits));
    }

    /**
     * Runs {@code DROP PROFILE ... CASCADE}; the users that had it get the DEFAULT profile.
     *
     * @throws IllegalArgumentException for the DEFAULT profile
     */
    public void drop(String name) {
        String profile = OracleIdentifiers.requireValid(name);
        if (DEFAULT.equals(profile)) {
            throw new IllegalArgumentException("El perfil DEFAULT no se puede eliminar");
        }
        jdbcTemplate.execute("DROP PROFILE " + profile + " CASCADE");
    }

    /** Builds {@code <resource> <value> ...} in the order of {@link #RESOURCES}, after validating every entry. */
    private static String limitClause(Map<String, String> limits) {
        if (limits == null || limits.isEmpty()) {
            throw new IllegalArgumentException("Establezca al menos un límite");
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        limits.forEach((resource, value) -> {
            String key = resource == null ? "" : resource.trim().toUpperCase(Locale.ROOT);
            if (!RESOURCES.contains(key)) {
                throw new IllegalArgumentException("Límite de perfil desconocido: " + resource);
            }
            normalized.put(key, requireLimitValue(key, value));
        });
        return RESOURCES.stream()
                .filter(normalized::containsKey)
                .map(resource -> resource + " " + normalized.get(resource))
                .collect(Collectors.joining(" "));
    }

    private static String requireLimitValue(String resource, String value) {
        String limit = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (limit.equals("UNLIMITED") || limit.equals(DEFAULT) || WHOLE_NUMBER.matcher(limit).matches()) {
            return limit;
        }
        throw new IllegalArgumentException(resource + " debe ser un número entero, UNLIMITED o DEFAULT");
    }

    public List<DbProfile> list() {
        Map<String, List<DbProfile.Limit>> byProfile = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(LIST_QUERY)) {
            byProfile.computeIfAbsent(String.valueOf(row.get("PROFILE")), name -> new ArrayList<>())
                    .add(new DbProfile.Limit(String.valueOf(row.get("RESOURCE_NAME")),
                            String.valueOf(row.get("RESOURCE_TYPE")), String.valueOf(row.get("LIMIT"))));
        }
        return byProfile.entrySet().stream()
                .map(entry -> new DbProfile(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
