package una.dbadmin.files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

/** Exercises the HTTP contract with the real service and a mocked {@link JdbcTemplate}; no database is needed. */
class FilesControllerTest {

    private static final String PATH_QUERY = "SELECT directory_path FROM dba_directories WHERE directory_name = ?";

    @TempDir
    Path backupDir;

    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        FilesController controller = new FilesController(new FilesService(jdbc));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsDirectories() throws Exception {
        when(jdbc.queryForList("SELECT directory_name, directory_path FROM dba_directories ORDER BY directory_name"))
                .thenReturn(List.of(Map.of("DIRECTORY_NAME", "BACKUPS", "DIRECTORY_PATH", "/u01/backups")));

        mvc.perform(get("/api/files/directories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data[0].name").value("BACKUPS"))
                .andExpect(jsonPath("$.data[0].path").value("/u01/backups"));
    }

    @Test
    void createsADirectory() throws Exception {
        mvc.perform(post("/api/files/directories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"backups\",\"path\":\"/u01/backups\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data").value("BACKUPS"));

        verify(jdbc).execute("CREATE OR REPLACE DIRECTORY BACKUPS AS '/u01/backups'");
    }

    @Test
    void createRejectsAPathWithAQuote() throws Exception {
        mvc.perform(post("/api/files/directories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"backups\",\"path\":\"/u01/it's\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void createRejectsAMissingName() throws Exception {
        mvc.perform(post("/api/files/directories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"/u01/backups\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void dropsADirectory() throws Exception {
        mvc.perform(delete("/api/files/directories/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("DROP DIRECTORY BACKUPS");
    }

    @Test
    void grantsAccess() throws Exception {
        mvc.perform(post("/api/files/directories/backups/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":\"scott\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("GRANT READ, WRITE ON DIRECTORY BACKUPS TO SCOTT");
    }

    @Test
    void grantRejectsAnInjectionAttempt() throws Exception {
        mvc.perform(post("/api/files/directories/backups/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user\":\"scott TO PUBLIC; --\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void revokesAccess() throws Exception {
        mvc.perform(delete("/api/files/directories/backups/grants/scott"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(jdbc).execute("REVOKE READ, WRITE ON DIRECTORY BACKUPS FROM SCOTT");
    }

    @Test
    void listsFiles() throws Exception {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("export.log"), "done");
        Files.writeString(backupDir.resolve("ignored.txt"), "x");

        mvc.perform(get("/api/files/directories/backups/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("export.log"))
                .andExpect(jsonPath("$.data[0].sizeBytes").value(4))
                .andExpect(jsonPath("$.data[0].lastModified").exists());
    }

    @Test
    void viewsALogFile() throws Exception {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("export.log"), "Export finished\n");

        mvc.perform(get("/api/files/directories/backups/files/export.log/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("export.log"))
                .andExpect(jsonPath("$.data.content").value("Export finished\n"))
                .andExpect(jsonPath("$.data.truncated").value(false));
    }

    @Test
    void refusesToViewADumpFile() throws Exception {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("data.dmp"), "binary");

        mvc.perform(get("/api/files/directories/backups/files/data.dmp/content"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void deleteWithoutConfirmationIsRejectedAndKeepsTheFile() throws Exception {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path dump = Files.writeString(backupDir.resolve("old.dmp"), "x");

        mvc.perform(delete("/api/files/directories/backups/files/old.dmp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        assertTrue(Files.exists(dump));
    }

    @Test
    void deleteWithConfirmationRemovesTheFile() throws Exception {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path dump = Files.writeString(backupDir.resolve("old.dmp"), "x");

        mvc.perform(delete("/api/files/directories/backups/files/old.dmp").param("confirm", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        assertFalse(Files.exists(dump));
    }

    @Test
    void deleteRejectsATraversalFileName() throws Exception {
        Path root = Files.createDirectory(backupDir.resolve("root"));
        Path outside = Files.writeString(backupDir.resolve("outside.dmp"), "secret");
        directoryIsMappedTo("BACKUPS", root);

        mvc.perform(delete("/api/files/directories/backups/files/..%5Coutside.dmp").param("confirm", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));

        assertTrue(Files.exists(outside));
    }

    @Test
    void anUnknownDirectoryIsABadRequest() throws Exception {
        when(jdbc.queryForList(PATH_QUERY, String.class, "MISSING")).thenReturn(List.of());

        mvc.perform(get("/api/files/directories/missing/files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void databaseFailuresKeepTheOracleCode() throws Exception {
        when(jdbc.queryForList(PATH_QUERY, String.class, "BACKUPS"))
                .thenThrow(new DataAccessResourceFailureException("ORA-01017: invalid username/password"));

        mvc.perform(get("/api/files/directories/backups/files"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("ORA-01017: invalid username/password"));
    }

    @Test
    void ioFailuresAreReportedAsAnOperationResult() throws Exception {
        FilesService failing = mock(FilesService.class);
        when(failing.listFiles("BACKUPS")).thenThrow(new java.io.UncheckedIOException(new IOException("disk error")));
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new FilesController(failing))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        failingMvc.perform(get("/api/files/directories/BACKUPS/files"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("File system error: disk error"));
    }

    private void directoryIsMappedTo(String name, Path path) {
        when(jdbc.queryForList(PATH_QUERY, String.class, name)).thenReturn(List.of(path.toString()));
    }
}
