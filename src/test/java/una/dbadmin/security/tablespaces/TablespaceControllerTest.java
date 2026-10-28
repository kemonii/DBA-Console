package una.dbadmin.security.tablespaces;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

/** Exercises the HTTP contract with the real service and a mocked {@link JdbcTemplate}; no database is needed. */
class TablespaceControllerTest {

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        TablespaceController controller = new TablespaceController(new TablespaceService(jdbc));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsTablespaces() throws Exception {
        when(jdbc.queryForList(TablespaceService.USAGE_QUERY)).thenReturn(List.of(Map.of(
                "TABLESPACE_NAME", "USERS",
                "TOTAL_MB", new BigDecimal("5"),
                "USED_MB", new BigDecimal("1.25"),
                "FREE_MB", new BigDecimal("3.75"),
                "USED_PERCENT", new BigDecimal("25"),
                "FREE_PERCENT", new BigDecimal("75"))));

        mvc.perform(get("/api/security/tablespaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data[0].name").value("USERS"))
                .andExpect(jsonPath("$.data[0].usedMb").value(1.25))
                .andExpect(jsonPath("$.data[0].usedPercent").value(25.0));
    }

    @Test
    void listsDatafiles() throws Exception {
        when(jdbc.queryForList(TablespaceService.DATAFILES_QUERY, "USERS")).thenReturn(List.of(Map.of(
                "FILE_ID", new BigDecimal("12"),
                "FILE_NAME", "/u01/oradata/XEPDB1/users01.dbf",
                "BYTES", new BigDecimal(5L * 1024 * 1024),
                "AUTOEXTENSIBLE", "NO",
                "MAXBYTES", BigDecimal.ZERO)));

        mvc.perform(get("/api/security/tablespaces/USERS/datafiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileId").value(12))
                .andExpect(jsonPath("$.data[0].sizeMb").value(5.0))
                .andExpect(jsonPath("$.data[0].autoextensible").value(false));
    }
}
