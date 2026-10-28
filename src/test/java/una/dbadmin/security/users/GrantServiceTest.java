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

class GrantServiceTest {

    private JdbcTemplate jdbc;
    private GrantService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new GrantService(jdbc);
    }

    private void systemPrivilegeExists(String name) {
        when(jdbc.queryForObject(GrantService.SYSTEM_PRIVILEGE_EXISTS_QUERY, Integer.class, name)).thenReturn(1);
    }

    private void maintainedByOracle(String grantee, boolean maintained) {
        when(jdbc.queryForObject(GrantService.MAINTAINED_GRANTEE_QUERY, Integer.class, grantee))
                .thenReturn(maintained ? 1 : 0);
    }

    // ---- listing -----------------------------------------------------------------------------------------------

    @Test
    void privilegesOfReadsTheThreeViews() {
        when(jdbc.queryForList(GrantService.SYSTEM_PRIVILEGES_QUERY, "PRUEBA")).thenReturn(List.of(
                Map.of("PRIVILEGE", "CREATE SESSION", "ADMIN_OPTION", "NO")));
        when(jdbc.queryForList(GrantService.ROLES_QUERY, "PRUEBA")).thenReturn(List.of(
                Map.of("GRANTED_ROLE", "VENTAS_ROL", "ADMIN_OPTION", "YES", "DEFAULT_ROLE", "YES")));
        when(jdbc.queryForList(GrantService.OBJECT_PRIVILEGES_QUERY, "PRUEBA")).thenReturn(List.of(
                Map.of("OWNER", "HR", "TABLE_NAME", "EMPLOYEES", "PRIVILEGE", "SELECT", "GRANTABLE", "NO")));

        GranteePrivileges result = service.privilegesOf("prueba");

        assertEquals(new GranteePrivileges("PRUEBA",
                List.of(new GranteePrivileges.SystemPrivilege("CREATE SESSION", false)),
                List.of(new GranteePrivileges.Role("VENTAS_ROL", true, true)),
                List.of(new GranteePrivileges.ObjectPrivilege("HR", "EMPLOYEES", "SELECT", false))), result);
    }

    // ---- system privileges -------------------------------------------------------------------------------------

    @Test
    void grantSystemPrivilegeNormalizesTheName() {
        systemPrivilegeExists("CREATE SESSION");

        service.grantSystemPrivilege("  create   session ", "prueba", false);

        verify(jdbc).execute("GRANT CREATE SESSION TO PRUEBA");
    }

    @Test
    void grantSystemPrivilegeWithAdminOption() {
        systemPrivilegeExists("CREATE TABLE");

        service.grantSystemPrivilege("CREATE TABLE", "ventas_rol", true);

        verify(jdbc).execute("GRANT CREATE TABLE TO VENTAS_ROL WITH ADMIN OPTION");
    }

    @Test
    void grantRejectsUnknownSystemPrivileges() {
        when(jdbc.queryForObject(GrantService.SYSTEM_PRIVILEGE_EXISTS_QUERY, Integer.class, "FLY")).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> service.grantSystemPrivilege("fly", "prueba", false));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantSystemPrivilege("DBA TO PRUEBA; --", "prueba", false));
        assertThrows(IllegalArgumentException.class, () -> service.grantSystemPrivilege(null, "prueba", false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void revokeSystemPrivilege() {
        systemPrivilegeExists("CREATE SESSION");
        maintainedByOracle("PRUEBA", false);

        service.revokeSystemPrivilege("create session", "prueba");

        verify(jdbc).execute("REVOKE CREATE SESSION FROM PRUEBA");
    }

    @Test
    void revokeRefusesGranteesMaintainedByOracle() {
        systemPrivilegeExists("CREATE SESSION");
        maintainedByOracle("SYSTEM", true);

        assertThrows(IllegalArgumentException.class, () -> service.revokeSystemPrivilege("CREATE SESSION", "system"));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- roles -------------------------------------------------------------------------------------------------

    @Test
    void grantRole() {
        service.grantRole("connect", "prueba", false);
        service.grantRole("ventas_rol", "public", true);

        verify(jdbc).execute("GRANT CONNECT TO PRUEBA");
        verify(jdbc).execute("GRANT VENTAS_ROL TO PUBLIC WITH ADMIN OPTION");
    }

    @Test
    void grantRoleRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.grantRole("dba to x", "prueba", false));
        assertThrows(IllegalArgumentException.class, () -> service.grantRole("dba", "x;", false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void revokeRole() {
        maintainedByOracle("PRUEBA", false);

        service.revokeRole("connect", "prueba");

        verify(jdbc).execute("REVOKE CONNECT FROM PRUEBA");
    }

    @Test
    void revokeRoleRefusesGranteesMaintainedByOracle() {
        maintainedByOracle("DBA", true);

        assertThrows(IllegalArgumentException.class, () -> service.revokeRole("select_catalog_role", "dba"));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- object privileges -------------------------------------------------------------------------------------

    @Test
    void grantObjectPrivilegesAsInClass() {
        service.grantObjectPrivileges(List.of("select", "insert", "update", "delete"), "hr", "employees", "prueba",
                false);

        verify(jdbc).execute("GRANT SELECT, INSERT, UPDATE, DELETE ON HR.EMPLOYEES TO PRUEBA");
    }

    @Test
    void grantObjectPrivilegesToPublicWithGrantOption() {
        service.grantObjectPrivileges(List.of("SELECT", "select"), "VENTAS", "PEDIDOS", "PUBLIC", true);

        verify(jdbc).execute("GRANT SELECT ON VENTAS.PEDIDOS TO PUBLIC WITH GRANT OPTION");
    }

    @Test
    void grantRejectsUnknownObjectPrivileges() {
        assertThrows(IllegalArgumentException.class,
                () -> service.grantObjectPrivileges(List.of(), "HR", "EMPLOYEES", "PRUEBA", false));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantObjectPrivileges(List.of("SELECT ON X TO Y; --"), "HR", "E", "P", false));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantObjectPrivileges(List.of("SELECT"), "HR", "EMPLOYEES x", "PRUEBA", false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void revokeObjectPrivileges() {
        maintainedByOracle("HR", false);
        maintainedByOracle("PRUEBA", false);

        service.revokeObjectPrivileges(List.of("SELECT"), "hr", "employees", "prueba");

        verify(jdbc).execute("REVOKE SELECT ON HR.EMPLOYEES FROM PRUEBA");
    }

    @Test
    void revokeRefusesObjectsOwnedByOracle() {
        maintainedByOracle("SYS", true);

        assertThrows(IllegalArgumentException.class,
                () -> service.revokeObjectPrivileges(List.of("EXECUTE"), "sys", "dbms_output", "public"));
        verify(jdbc, never()).execute(anyString());
    }
}
