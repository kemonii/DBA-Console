package una.dbadmin.security.auditing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

/** Exercises the HTTP contract with the real service and a mocked {@link JdbcTemplate}; no database is needed. */
class AuditControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuditController(new AuditService(jdbc)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void showsTheAuditTrailParameter() throws Exception {
        when(jdbc.queryForList(AuditService.PARAMETER_QUERY, String.class)).thenReturn(List.of("DB"));

        mvc.perform(get("/api/security/auditing/parameter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("DB"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void listsTheAuditOptions() throws Exception {
        when(jdbc.queryForList(AuditService.OBJECT_OPTIONS_QUERY)).thenReturn(List.of(Map.of(
                "OWNER", "HR", "OBJECT_NAME", "JOBS", "OBJECT_TYPE", "TABLE",
                "INS", "A/A", "UPD", "A/A", "DEL", "A/A", "SEL", "-/-")));

        mvc.perform(get("/api/security/auditing/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statementOptions").isEmpty())
                .andExpect(jsonPath("$.data.objectOptions[0].objectName").value("JOBS"))
                .andExpect(jsonPath("$.data.objectOptions[0].insert").value("A/A"));
    }
}
