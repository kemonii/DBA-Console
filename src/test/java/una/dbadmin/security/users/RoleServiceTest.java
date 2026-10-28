package una.dbadmin.security.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RoleServiceTest {

    private JdbcTemplate jdbc;
    private RoleService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new RoleService(jdbc);
    }

    @Test
    void listMarksTheRolesMaintainedByOracle() {
        when(jdbc.queryForList(RoleService.LIST_QUERY)).thenReturn(List.of(
                Map.of("ROLE", "VENTAS_ROL", "ORACLE_MAINTAINED", "N"),
                Map.of("ROLE", "CONNECT", "ORACLE_MAINTAINED", "Y")));

        assertEquals(List.of(new DbRole("VENTAS_ROL", false), new DbRole("CONNECT", true)), service.list());
    }

    @Test
    void createRunsCreateRole() {
        assertEquals("VENTAS_ROL", service.create("ventas_rol"));

        verify(jdbc).execute("CREATE ROLE VENTAS_ROL");
    }

    @Test
    void createRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.create("rol; DROP USER x"));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropRunsDropRole() {
        when(jdbc.queryForList(RoleService.MAINTAINED_QUERY, String.class, "VENTAS_ROL")).thenReturn(List.of("N"));

        service.drop("ventas_rol");

        verify(jdbc).execute("DROP ROLE VENTAS_ROL");
    }

    @Test
    void dropRefusesRolesMaintainedByOracle() {
        when(jdbc.queryForList(RoleService.MAINTAINED_QUERY, String.class, "DBA")).thenReturn(List.of("Y"));

        assertThrows(IllegalArgumentException.class, () -> service.drop("dba"));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropReportsAMissingRole() {
        when(jdbc.queryForList(RoleService.MAINTAINED_QUERY, String.class, "NINGUNO")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.drop("ninguno"));
        verify(jdbc, never()).execute(anyString());
    }
}
