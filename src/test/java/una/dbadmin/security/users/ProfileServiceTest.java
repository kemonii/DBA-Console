package una.dbadmin.security.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
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

    // ---- create, alter, drop -----------------------------------------------------------------------------------

    @Test
    void createWritesTheLimitsInAFixedOrder() {
        Map<String, String> limits = new LinkedHashMap<>();
        limits.put("password_life_time", "30");
        limits.put("SESSIONS_PER_USER", "2");
        limits.put("connect_time", "unlimited");
        limits.put("FAILED_LOGIN_ATTEMPTS", " 3 ");
        limits.put("PASSWORD_LOCK_TIME", "default");

        assertEquals("APP_USER", service.create("app_user", limits));

        verify(jdbc).execute("CREATE PROFILE APP_USER LIMIT SESSIONS_PER_USER 2 CONNECT_TIME UNLIMITED "
                + "FAILED_LOGIN_ATTEMPTS 3 PASSWORD_LIFE_TIME 30 PASSWORD_LOCK_TIME DEFAULT");
    }

    @Test
    void alterTheDefaultProfileAsInClass() {
        service.alter("default", Map.of("IDLE_TIME", "20"));

        verify(jdbc).execute("ALTER PROFILE DEFAULT LIMIT IDLE_TIME 20");
    }

    @Test
    void createRejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> service.create("P", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> service.create("P", null));
        assertThrows(IllegalArgumentException.class, () -> service.create("P", Map.of("PRIVATE_SGA", "20M")));
        assertThrows(IllegalArgumentException.class, () -> service.create("P", Map.of("IDLE_TIME", "0")));
        assertThrows(IllegalArgumentException.class, () -> service.create("P", Map.of("IDLE_TIME", "-1")));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("P", Map.of("IDLE_TIME", "30 SESSIONS_PER_USER 1")));
        assertThrows(IllegalArgumentException.class, () -> service.create("bad name", Map.of("IDLE_TIME", "30")));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropCascades() {
        service.drop("app_user");

        verify(jdbc).execute("DROP PROFILE APP_USER CASCADE");
    }

    @Test
    void dropRefusesTheDefaultProfile() {
        assertThrows(IllegalArgumentException.class, () -> service.drop("default"));
        verify(jdbc, never()).execute(anyString());
    }
}
