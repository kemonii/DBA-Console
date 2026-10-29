package una.dbadmin.security.tablespaces;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

/** Exercises the HTTP contract with the real service and a mocked {@link JdbcTemplate}; no database is needed. */
class TablespaceControllerTest {

    private static final String DATAFILE_DIR = "/u01/oradata/XEPDB1";

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        TablespaceController controller = new TablespaceController(new TablespaceService(jdbc, DATAFILE_DIR));
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
    void createsATablespace() throws Exception {
        mvc.perform(post("/api/security/tablespaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ventas","fileName":"ventas01.dbf","sizeMb":2,"nextMb":1,"maxSizeMb":1024}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").value("VENTAS"));

        verify(jdbc).execute("CREATE TABLESPACE VENTAS DATAFILE '"
                + Path.of(DATAFILE_DIR).resolve("ventas01.dbf") + "' SIZE 2M AUTOEXTEND ON NEXT 1M MAXSIZE 1024M");
    }

    @Test
    void createRejectsAPathInTheFileName() throws Exception {
        mvc.perform(post("/api/security/tablespaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ventas","fileName":"../../system01.dbf","sizeMb":2,"nextMb":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createRejectsAMissingSize() throws Exception {
        mvc.perform(post("/api/security/tablespaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ventas","fileName":"ventas01.dbf","nextMb":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tamaño inicial es obligatorio"));
    }

    @Test
    void createReportsTheOracleError() throws Exception {
        doThrow(new DataIntegrityViolationException("x",
                        new SQLException("ORA-01543: tablespace 'VENTAS' already exists")))
                .when(jdbc).execute(anyString());

        mvc.perform(post("/api/security/tablespaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ventas","fileName":"ventas01.dbf","sizeMb":2,"nextMb":1}"""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("ORA-01543: tablespace 'VENTAS' already exists"));
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

    @Test
    void addsADatafile() throws Exception {
        mvc.perform(post("/api/security/tablespaces/VENTAS/datafiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"ventas02.dbf\",\"sizeMb\":256}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("ALTER TABLESPACE VENTAS ADD DATAFILE '"
                + Path.of(DATAFILE_DIR).resolve("ventas02.dbf") + "' SIZE 256M");
    }

    @Test
    void resizesADatafile() throws Exception {
        when(jdbc.queryForList(TablespaceService.DATAFILE_PATH_QUERY, String.class, 12, "VENTAS"))
                .thenReturn(List.of("/u01/oradata/XEPDB1/ventas01.dbf"));

        mvc.perform(post("/api/security/tablespaces/VENTAS/datafiles/12/resize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sizeMb\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("ALTER DATABASE DATAFILE '/u01/oradata/XEPDB1/ventas01.dbf' RESIZE 5M");
    }

    @Test
    void dropRequiresConfirmation() throws Exception {
        mvc.perform(delete("/api/security/tablespaces/VENTAS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropsATablespaceWhenConfirmed() throws Exception {
        mvc.perform(delete("/api/security/tablespaces/VENTAS").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("DROP TABLESPACE VENTAS INCLUDING CONTENTS AND DATAFILES");
    }
}
