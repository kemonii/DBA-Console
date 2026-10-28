package una.dbadmin.security.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class UserServiceTest {

    private JdbcTemplate jdbc;
    private UserService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new UserService(jdbc);
    }

    private void userExists(String user, String oracleMaintained) {
        when(jdbc.queryForList(UserService.MAINTAINED_QUERY, String.class, user)).thenReturn(List.of(oracleMaintained));
    }

    // ---- list --------------------------------------------------------------------------------------------------

    @Test
    void listMapsDbaUsers() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 27, 10, 30);
        Map<String, Object> row = new HashMap<>();
        row.put("USERNAME", "PRUEBA");
        row.put("ACCOUNT_STATUS", "OPEN");
        row.put("DEFAULT_TABLESPACE", "USERS");
        row.put("TEMPORARY_TABLESPACE", "TEMP");
        row.put("PROFILE", "DEFAULT");
        row.put("CREATED", Timestamp.valueOf(created));
        when(jdbc.queryForList(UserService.LIST_QUERY)).thenReturn(List.of(row));

        assertEquals(List.of(new DbUser("PRUEBA", "OPEN", "USERS", "TEMP", "DEFAULT", created)), service.list());
    }

    // ---- create ------------------------------------------------------------------------------------------------

    @Test
    void createWithOnlyAPassword() {
        assertEquals("PRUEBA", service.create("prueba", "Secreta1", null, null, null, false));

        verify(jdbc).execute("CREATE USER PRUEBA IDENTIFIED BY \"Secreta1\"");
    }

    @Test
    void createWithTablespacesAndQuota() {
        service.create("prueba", "Secreta1", "users", "temp", 100, false);

        verify(jdbc).execute("CREATE USER PRUEBA IDENTIFIED BY \"Secreta1\" DEFAULT TABLESPACE USERS "
                + "TEMPORARY TABLESPACE TEMP QUOTA 100M ON USERS");
    }

    @Test
    void createWithUnlimitedQuota() {
        service.create("prueba", "Secreta1", "users", "", null, true);

        verify(jdbc).execute("CREATE USER PRUEBA IDENTIFIED BY \"Secreta1\" DEFAULT TABLESPACE USERS "
                + "QUOTA UNLIMITED ON USERS");
    }

    @Test
    void createRejectsAQuotaWithoutDefaultTablespace() {
        assertThrows(IllegalArgumentException.class, () -> service.create("p", "x", null, null, 10, false));
        assertThrows(IllegalArgumentException.class, () -> service.create("p", "x", " ", null, null, true));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createRejectsInvalidQuotas() {
        assertThrows(IllegalArgumentException.class, () -> service.create("p", "x", "USERS", null, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("p", "x", "USERS", null, UserService.MAX_QUOTA_MB + 1, false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createRejectsUnsafePasswords() {
        for (String password : new String[] {null, "", "  ", "a\"; DROP USER SYSTEM; --", "line\nbreak",
                "x".repeat(UserService.MAX_PASSWORD_LENGTH + 1)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create("prueba", password, null, null, null, false), "accepted " + password);
        }
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.create("bad name", "x", null, null, null, false));
        assertThrows(IllegalArgumentException.class, () -> service.create("p", "x", "users;", null, null, false));
        assertThrows(IllegalArgumentException.class, () -> service.create("p", "x", null, "te mp", null, false));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- lock --------------------------------------------------------------------------------------------------

    @Test
    void lockAndUnlock() {
        userExists("PRUEBA", "N");

        service.setLocked("prueba", true);
        service.setLocked("prueba", false);

        verify(jdbc).execute("ALTER USER PRUEBA ACCOUNT LOCK");
        verify(jdbc).execute("ALTER USER PRUEBA ACCOUNT UNLOCK");
    }

    @Test
    void lockRefusesUsersMaintainedByOracle() {
        userExists("SYSTEM", "Y");

        assertThrows(IllegalArgumentException.class, () -> service.setLocked("system", true));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- drop --------------------------------------------------------------------------------------------------

    @Test
    void dropRunsDropUser() {
        userExists("PRUEBA", "N");

        service.drop("prueba", false, true);

        verify(jdbc).execute("DROP USER PRUEBA");
    }

    @Test
    void dropWithCascade() {
        userExists("PRUEBA", "N");

        service.drop("prueba", true, true);

        verify(jdbc).execute("DROP USER PRUEBA CASCADE");
    }

    @Test
    void dropRequiresConfirmation() {
        userExists("PRUEBA", "N");

        assertThrows(IllegalArgumentException.class, () -> service.drop("prueba", true, false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropRefusesUsersMaintainedByOracle() {
        userExists("SYSTEM", "Y");

        assertThrows(IllegalArgumentException.class, () -> service.drop("system", true, true));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropReportsAMissingUser() {
        when(jdbc.queryForList(UserService.MAINTAINED_QUERY, String.class, "NADIE")).thenReturn(List.of());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.drop("nadie", false, true));
        assertEquals("User NADIE does not exist", ex.getMessage());
    }
}
