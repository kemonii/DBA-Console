package una.dbadmin.security.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProfileServiceTest {

    private JdbcTemplate jdbc;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new ProfileService(jdbc);
    }

    private static Map<String, Object> row(String profile, String resource, String type, String limit) {
        return Map.of("PROFILE", profile, "RESOURCE_NAME", resource, "RESOURCE_TYPE", type, "LIMIT", limit);
    }

    // ---- list --------------------------------------------------------------------------------------------------

    @Test
    void listGroupsTheLimitsByProfile() {
        when(jdbc.queryForList(ProfileService.LIST_QUERY)).thenReturn(List.of(
                row("APP_USER", "SESSIONS_PER_USER", "KERNEL", "2"),
                row("APP_USER", "FAILED_LOGIN_ATTEMPTS", "PASSWORD", "3"),
                row("DEFAULT", "SESSIONS_PER_USER", "KERNEL", "UNLIMITED")));

        assertEquals(List.of(
                new DbProfile("APP_USER", List.of(
                        new DbProfile.Limit("SESSIONS_PER_USER", "KERNEL", "2"),
                        new DbProfile.Limit("FAILED_LOGIN_ATTEMPTS", "PASSWORD", "3"))),
                new DbProfile("DEFAULT", List.of(
                        new DbProfile.Limit("SESSIONS_PER_USER", "KERNEL", "UNLIMITED")))), service.list());
    }
}
