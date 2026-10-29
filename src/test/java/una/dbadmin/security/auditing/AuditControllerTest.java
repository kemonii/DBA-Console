package una.dbadmin.security.auditing;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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

    @Test
    void enablesAndDisablesSessionAuditing() throws Exception {
        mvc.perform(post("/api/security/auditing/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whenever\":\"NOT_SUCCESSFUL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Auditoría de sesiones activada"));
        mvc.perform(delete("/api/security/auditing/session"))
                .andExpect(status().isOk());

        verify(jdbc).execute("AUDIT SESSION WHENEVER NOT SUCCESSFUL");
        verify(jdbc).execute("NOAUDIT SESSION");
    }

    @Test
    void enablesAndDisablesTableAuditing() throws Exception {
        mvc.perform(post("/api/security/auditing/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"owner":"HR","table":"JOBS","actions":["INSERT","UPDATE","DELETE"],"byAccess":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Auditoría de la tabla activada"));
        mvc.perform(delete("/api/security/auditing/tables")
                        .param("owner", "HR")
                        .param("table", "JOBS")
                        .param("actions", "INSERT,UPDATE,DELETE"))
                .andExpect(status().isOk());

        verify(jdbc).execute("AUDIT INSERT, UPDATE, DELETE ON HR.JOBS BY ACCESS");
        verify(jdbc).execute("NOAUDIT INSERT, UPDATE, DELETE ON HR.JOBS");
    }

    @Test
    void rejectsAnUnknownTableAction() throws Exception {
        mvc.perform(post("/api/security/auditing/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"HR\",\"table\":\"JOBS\",\"actions\":[\"TRUNCATE\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Acción de tabla desconocida: TRUNCATE"));
    }

    @Test
    void readsTheTrailWithFilters() throws Exception {
        when(jdbc.queryForList(AuditService.TRAIL_QUERY, "PRUEBA", "PRUEBA", 10))
                .thenReturn(List.of(Map.of("USERNAME", "PRUEBA", "ACTION_NAME", "LOGON")));

        mvc.perform(get("/api/security/auditing/trail").param("username", "prueba").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].actionName").value("LOGON"));
    }

    @Test
    void readsSessionsAndObjects() throws Exception {
        when(jdbc.queryForList(AuditService.SESSIONS_QUERY, (Object) null, null, AuditService.DEFAULT_LIMIT))
                .thenReturn(List.of(Map.of("USERNAME", "PRUEBA", "OUTCOME", "Connected")));
        when(jdbc.queryForList(AuditService.OBJECTS_QUERY, (Object) null, null, AuditService.DEFAULT_LIMIT))
                .thenReturn(List.of(Map.of("OBJ_NAME", "JOBS", "OUTCOME", "Done")));

        mvc.perform(get("/api/security/auditing/trail/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].outcome").value("Connected"));
        mvc.perform(get("/api/security/auditing/trail/objects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].objectName").value("JOBS"));
    }

    @Test
    void rejectsAnUnknownWhenever() throws Exception {
        mvc.perform(delete("/api/security/auditing/session").param("whenever", "SOMETIMES"))
                .andExpect(status().isBadRequest());

        verify(jdbc, never()).execute(anyString());
    }
}
