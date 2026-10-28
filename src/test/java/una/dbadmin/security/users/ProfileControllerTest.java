package una.dbadmin.security.users;

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
}
