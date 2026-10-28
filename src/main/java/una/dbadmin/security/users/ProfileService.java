package una.dbadmin.security.users;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Profiles with the statements from the course ("Seguridad": a profile limits the resources and passwords of the
 * users that have it).
 */
@Service
public class ProfileService {

    static final String LIST_QUERY = "SELECT profile, resource_name, resource_type, limit FROM dba_profiles "
            + "ORDER BY profile, resource_type, resource_name";

    private final JdbcTemplate jdbcTemplate;

    public ProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
