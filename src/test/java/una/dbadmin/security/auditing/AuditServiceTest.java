package una.dbadmin.security.auditing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
