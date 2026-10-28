package una.dbadmin.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

class BackupControllerTest {

    private static final String BODY = """
            {"type":"SCHEMA","schema":"hr","directory":"respaldo","fileName":"hr"}""";

    private final BackupService service = mock(BackupService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BackupController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void successfulBackupReturnsOkWithResult() throws Exception {
        when(service.backup(any())).thenReturn(
                new BackupResult(true, 0, "expdp system/******@//localhost:1521/XEPDB1", List.of("Job completed")));

        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.exitCode").value(0))
                .andExpect(jsonPath("$.data.command").value("expdp system/******@//localhost:1521/XEPDB1"))
                .andExpect(jsonPath("$.data.output[0]").value("Job completed"));

        verify(service).backup(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr"));
    }

    @Test
    void tableFiltersInTheJsonReachTheService() throws Exception {
        when(service.backup(any())).thenReturn(new BackupResult(true, 0, "expdp", List.of()));
        String body = """
                {"type":"SCHEMA","schema":"hr","directory":"respaldo","fileName":"hr","excludeTables":["audit_log"]}""";

        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(service).backup(
                new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", null, List.of("audit_log")));
    }

    @Test
    void missingOracleUtilitiesAreReportedAsAnOperationResult() throws Exception {
        when(service.backup(any())).thenThrow(new UncheckedIOException(
                "Could not run expdp; check that it is installed and on the PATH", new IOException("not found")));

        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Could not run expdp; check that it is installed and on the PATH"));
    }

    @Test
    void failedBackupReturnsServerErrorWithOutput() throws Exception {
        when(service.backup(any())).thenReturn(
                new BackupResult(false, 1, "expdp system/******@//localhost:1521/XEPDB1", List.of("ORA-39002")));

        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.data.output[0]").value("ORA-39002"));
    }

    @Test
    void invalidInputReturnsBadRequest() throws Exception {
        when(service.backup(any())).thenThrow(new IllegalArgumentException("Invalid Oracle identifier: x y"));

        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Invalid Oracle identifier: x y"));
    }

    @Test
    void missingBodyIsRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/backup").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
