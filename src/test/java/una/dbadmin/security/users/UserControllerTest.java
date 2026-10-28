package una.dbadmin.security.users;

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

import java.util.HashMap;
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
class UserControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        mvc = MockMvcBuilders.standaloneSetup(new UserController(new UserService(jdbc)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsUsers() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("USERNAME", "PRUEBA");
        row.put("ACCOUNT_STATUS", "OPEN");
        row.put("DEFAULT_TABLESPACE", "USERS");
        row.put("TEMPORARY_TABLESPACE", "TEMP");
        row.put("PROFILE", "DEFAULT");
        when(jdbc.queryForList(UserService.LIST_QUERY)).thenReturn(List.of(row));

        mvc.perform(get("/api/security/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("PRUEBA"))
                .andExpect(jsonPath("$.data[0].accountStatus").value("OPEN"));
    }

    @Test
    void createsAUser() throws Exception {
        mvc.perform(post("/api/security/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"prueba","password":"Secreta1","defaultTablespace":"USERS","quotaMb":50}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PRUEBA"));

        verify(jdbc).execute("CREATE USER PRUEBA IDENTIFIED BY \"Secreta1\" DEFAULT TABLESPACE USERS QUOTA 50M ON USERS");
    }

    @Test
    void createRejectsAPasswordWithQuotes() throws Exception {
        mvc.perform(post("/api/security/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"prueba","password":"a\\"b"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password cannot contain double quotes or control characters"));

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropRequiresConfirmation() throws Exception {
        when(jdbc.queryForList(UserService.MAINTAINED_QUERY, String.class, "PRUEBA")).thenReturn(List.of("N"));

        mvc.perform(delete("/api/security/users/PRUEBA"))
                .andExpect(status().isBadRequest());

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropsAUserWithCascade() throws Exception {
        when(jdbc.queryForList(UserService.MAINTAINED_QUERY, String.class, "PRUEBA")).thenReturn(List.of("N"));

        mvc.perform(delete("/api/security/users/PRUEBA").param("cascade", "true").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("DROP USER PRUEBA CASCADE");
    }
}
