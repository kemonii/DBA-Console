package una.dbadmin.security.users;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ProfileControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProfileController(new ProfileService(jdbc)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsProfiles() throws Exception {
        when(jdbc.queryForList(ProfileService.LIST_QUERY)).thenReturn(List.of(Map.of(
                "PROFILE", "DEFAULT", "RESOURCE_NAME", "IDLE_TIME", "RESOURCE_TYPE", "KERNEL", "LIMIT", "UNLIMITED")));

        mvc.perform(get("/api/security/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("DEFAULT"))
                .andExpect(jsonPath("$.data[0].limits[0].resource").value("IDLE_TIME"));
    }

    @Test
    void createsAltersAndDropsAProfile() throws Exception {
        mvc.perform(post("/api/security/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"app_user","limits":{"SESSIONS_PER_USER":"2","IDLE_TIME":"30"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("APP_USER"));
        mvc.perform(put("/api/security/profiles/APP_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limits\":{\"IDLE_TIME\":\"UNLIMITED\"}}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/security/profiles/APP_USER"))
                .andExpect(status().isOk());

        verify(jdbc).execute("CREATE PROFILE APP_USER LIMIT SESSIONS_PER_USER 2 IDLE_TIME 30");
        verify(jdbc).execute("ALTER PROFILE APP_USER LIMIT IDLE_TIME UNLIMITED");
        verify(jdbc).execute("DROP PROFILE APP_USER CASCADE");
    }

    @Test
    void rejectsAnUnknownLimit() throws Exception {
        mvc.perform(post("/api/security/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"p\",\"limits\":{\"COMPOSITE_LIMIT\":\"5\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown profile limit: COMPOSITE_LIMIT"));
    }
}
