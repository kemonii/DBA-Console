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
class GrantControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        mvc = MockMvcBuilders.standaloneSetup(new GrantController(new GrantService(jdbc)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsSystemPrivilegeNames() throws Exception {
        when(jdbc.queryForList(GrantService.SYSTEM_PRIVILEGE_NAMES_QUERY, String.class))
                .thenReturn(List.of("CREATE SESSION", "CREATE TABLE"));

        mvc.perform(get("/api/security/grants/system-privileges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1]").value("CREATE TABLE"));
    }

    @Test
    void showsThePrivilegesOfAGrantee() throws Exception {
        when(jdbc.queryForList(GrantService.SYSTEM_PRIVILEGES_QUERY, "PRUEBA"))
                .thenReturn(List.of(Map.of("PRIVILEGE", "CREATE SESSION", "ADMIN_OPTION", "NO")));
        when(jdbc.queryForList(GrantService.ROLES_QUERY, "PRUEBA")).thenReturn(List.of());

        mvc.perform(get("/api/security/grants/PRUEBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grantee").value("PRUEBA"))
                .andExpect(jsonPath("$.data.systemPrivileges[0].privilege").value("CREATE SESSION"));
    }

    @Test
    void grantsASystemPrivilege() throws Exception {
        when(jdbc.queryForObject(GrantService.SYSTEM_PRIVILEGE_EXISTS_QUERY, Integer.class, "CREATE SESSION"))
                .thenReturn(1);

        mvc.perform(post("/api/security/grants/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"privilege\":\"CREATE SESSION\",\"grantee\":\"prueba\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Privilege granted"));

        verify(jdbc).execute("GRANT CREATE SESSION TO PRUEBA");
    }

    @Test
    void revokesASystemPrivilegeWithQueryParameters() throws Exception {
        when(jdbc.queryForObject(GrantService.SYSTEM_PRIVILEGE_EXISTS_QUERY, Integer.class, "CREATE SESSION"))
                .thenReturn(1);
        when(jdbc.queryForObject(GrantService.MAINTAINED_GRANTEE_QUERY, Integer.class, "PRUEBA")).thenReturn(0);

        mvc.perform(delete("/api/security/grants/system")
                        .param("privilege", "CREATE SESSION")
                        .param("grantee", "PRUEBA"))
                .andExpect(status().isOk());

        verify(jdbc).execute("REVOKE CREATE SESSION FROM PRUEBA");
    }

    @Test
    void grantsAndRevokesARole() throws Exception {
        when(jdbc.queryForObject(GrantService.MAINTAINED_GRANTEE_QUERY, Integer.class, "PRUEBA")).thenReturn(0);

        mvc.perform(post("/api/security/grants/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONNECT\",\"grantee\":\"PRUEBA\",\"adminOption\":false}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/security/grants/role").param("role", "CONNECT").param("grantee", "PRUEBA"))
                .andExpect(status().isOk());

        verify(jdbc).execute("GRANT CONNECT TO PRUEBA");
        verify(jdbc).execute("REVOKE CONNECT FROM PRUEBA");
    }
}
