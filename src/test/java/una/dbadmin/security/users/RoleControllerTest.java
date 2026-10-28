package una.dbadmin.security.users;

import static org.mockito.Mockito.mock;
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
class RoleControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        mvc = MockMvcBuilders.standaloneSetup(new RoleController(new RoleService(jdbc)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsRoles() throws Exception {
        when(jdbc.queryForList(RoleService.LIST_QUERY))
                .thenReturn(List.of(Map.of("ROLE", "CONNECT", "ORACLE_MAINTAINED", "Y")));

        mvc.perform(get("/api/security/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CONNECT"))
                .andExpect(jsonPath("$.data[0].oracleMaintained").value(true));
    }

    @Test
    void createsARole() throws Exception {
        mvc.perform(post("/api/security/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ventas_rol\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("VENTAS_ROL"));

        verify(jdbc).execute("CREATE ROLE VENTAS_ROL");
    }

    @Test
    void refusesToDropAnOracleRole() throws Exception {
        when(jdbc.queryForList(RoleService.MAINTAINED_QUERY, String.class, "DBA")).thenReturn(List.of("Y"));

        mvc.perform(delete("/api/security/roles/DBA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
