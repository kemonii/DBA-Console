package una.dbadmin.security.auditing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditServiceTest {

    private JdbcTemplate jdbc;
    private AuditService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new AuditService(jdbc);
    }

    // ---- parameter and options ---------------------------------------------------------------------------------

    @Test
    void auditTrailDbIsEnabled() {
        when(jdbc.queryForList(AuditService.PARAMETER_QUERY, String.class)).thenReturn(List.of("DB"));

        assertEquals(new AuditTrailParameter("DB", true), service.auditTrail());
    }

    @Test
    void auditTrailDbExtendedIsEnabled() {
        when(jdbc.queryForList(AuditService.PARAMETER_QUERY, String.class)).thenReturn(List.of("DB, EXTENDED"));

        assertEquals(new AuditTrailParameter("DB, EXTENDED", true), service.auditTrail());
    }

    @Test
    void auditTrailNoneOrOsDoesNotWriteToTheDatabase() {
        when(jdbc.queryForList(AuditService.PARAMETER_QUERY, String.class)).thenReturn(List.of("NONE"), List.of("OS"));

        assertEquals(new AuditTrailParameter("NONE", false), service.auditTrail());
        assertEquals(new AuditTrailParameter("OS", false), service.auditTrail());
    }

    @Test
    void optionsReadStatementAndObjectOptions() {
        Map<String, Object> statement = new HashMap<>();
        statement.put("USER_NAME", null);
        statement.put("AUDIT_OPTION", "CREATE SESSION");
        statement.put("SUCCESS", "BY ACCESS");
        statement.put("FAILURE", "BY ACCESS");
        when(jdbc.queryForList(AuditService.STATEMENT_OPTIONS_QUERY)).thenReturn(List.of(statement));
        when(jdbc.queryForList(AuditService.OBJECT_OPTIONS_QUERY)).thenReturn(List.of(Map.of(
                "OWNER", "HR", "OBJECT_NAME", "JOBS", "OBJECT_TYPE", "TABLE",
                "INS", "A/A", "UPD", "A/A", "DEL", "A/A", "SEL", "-/-")));

        AuditOptions options = service.options();

        assertEquals(List.of(new AuditOptions.StatementOption(null, "CREATE SESSION", "BY ACCESS", "BY ACCESS")),
                options.statementOptions());
        assertEquals(List.of(new AuditOptions.ObjectOption("HR", "JOBS", "TABLE", "A/A", "A/A", "A/A", "-/-")),
                options.objectOptions());
    }

    // ---- sessions ----------------------------------------------------------------------------------------------

    @Test
    void auditSessionsAsInClass() {
        service.auditSessions(Whenever.ALWAYS);
        service.auditSessions(Whenever.SUCCESSFUL);
        service.auditSessions(Whenever.NOT_SUCCESSFUL);
        service.auditSessions(null);

        verify(jdbc, times(2)).execute("AUDIT SESSION");
        verify(jdbc).execute("AUDIT SESSION WHENEVER SUCCESSFUL");
        verify(jdbc).execute("AUDIT SESSION WHENEVER NOT SUCCESSFUL");
    }

    @Test
    void stopAuditingSessions() {
        service.stopAuditingSessions(Whenever.ALWAYS);
        service.stopAuditingSessions(Whenever.NOT_SUCCESSFUL);

        verify(jdbc).execute("NOAUDIT SESSION");
        verify(jdbc).execute("NOAUDIT SESSION WHENEVER NOT SUCCESSFUL");
    }

    // ---- tables ------------------------------------------------------------------------------------------------

    @Test
    void auditTableAsInClass() {
        service.auditTable("hr", "jobs", List.of("insert", "update", "delete"), true);

        verify(jdbc).execute("AUDIT INSERT, UPDATE, DELETE ON HR.JOBS BY ACCESS");
    }

    @Test
    void auditTableBySession() {
        service.auditTable("HR", "EMPLOYEES", List.of("SELECT", "select"), false);

        verify(jdbc).execute("AUDIT SELECT ON HR.EMPLOYEES BY SESSION");
    }

    @Test
    void stopAuditingTable() {
        service.stopAuditingTable("hr", "jobs", List.of("INSERT", "DELETE"));

        verify(jdbc).execute("NOAUDIT INSERT, DELETE ON HR.JOBS");
    }

    // ---- trail -------------------------------------------------------------------------------------------------

    @Test
    void trailOfEveryUserWithTheDefaultLimit() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 27, 21, 5);
        Map<String, Object> row = new HashMap<>();
        row.put("TIMESTAMP", Timestamp.valueOf(at));
        row.put("USERNAME", "PRUEBA");
        row.put("ACTION_NAME", "INSERT");
        row.put("OWNER", "HR");
        row.put("OBJ_NAME", "JOBS");
        row.put("PRIV_USED", null);
        row.put("RETURNCODE", new BigDecimal("0"));
        when(jdbc.queryForList(AuditService.TRAIL_QUERY, (Object) null, null, AuditService.DEFAULT_LIMIT))
                .thenReturn(List.of(row));

        assertEquals(List.of(new AuditEntries.TrailEntry(at, "PRUEBA", "INSERT", "HR", "JOBS", null, 0)),
                service.trail(" ", null));
    }

    @Test
    void sessionsOfOneUser() {
        Map<String, Object> row = new HashMap<>();
        row.put("USERNAME", "PRUEBA");
        row.put("OS_USERNAME", "monica");
        row.put("TERMINAL", "TUF");
        row.put("OUTCOME", "Failed");
        when(jdbc.queryForList(AuditService.SESSIONS_QUERY, "PRUEBA", "PRUEBA", 20)).thenReturn(List.of(row));

        assertEquals(List.of(new AuditEntries.SessionEntry(null, null, "monica", "PRUEBA", "TUF", "Failed")),
                service.sessions("prueba", 20));
    }

    @Test
    void objectsAreRead() {
        when(jdbc.queryForList(AuditService.OBJECTS_QUERY, (Object) null, null, 5)).thenReturn(List.of(Map.of(
                "USERNAME", "PRUEBA", "OWNER", "HR", "OBJ_NAME", "JOBS", "ACTION_NAME", "UPDATE",
                "OUTCOME", "Done")));

        assertEquals(List.of(new AuditEntries.ObjectEntry(null, null, "PRUEBA", null, "HR", "JOBS", "UPDATE", "Done")),
                service.objects(null, 5));
    }

    @Test
    void trailRejectsInvalidFilters() {
        assertThrows(IllegalArgumentException.class, () -> service.trail("x' OR '1'='1", null));
        assertThrows(IllegalArgumentException.class, () -> service.trail(null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.sessions(null, AuditService.MAX_LIMIT + 1));
    }

    @Test
    void auditTableRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> service.auditTable("HR", "JOBS", List.of(), true));
        assertThrows(IllegalArgumentException.class, () -> service.auditTable("HR", "JOBS", null, true));
        assertThrows(IllegalArgumentException.class,
                () -> service.auditTable("HR", "JOBS", List.of("DROP"), true));
        assertThrows(IllegalArgumentException.class,
                () -> service.auditTable("HR", "JOBS; DROP USER X", List.of("INSERT"), true));
        assertThrows(IllegalArgumentException.class,
                () -> service.stopAuditingTable("H R", "JOBS", List.of("INSERT")));
        verify(jdbc, never()).execute(anyString());
    }
}
