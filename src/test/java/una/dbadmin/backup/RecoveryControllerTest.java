package una.dbadmin.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

class RecoveryControllerTest {

    private static final String BODY = """
            {"type":"SCHEMA","schema":"hr","directory":"respaldo","fileName":"hr"}""";

    private final RecoveryService service = mock(RecoveryService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RecoveryController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void successfulRecoveryReturnsOkWithResult() throws Exception {
        when(service.recover(any())).thenReturn(
                new RecoveryResult(true, 0, "impdp system/******@//localhost:1521/XEPDB1", List.of("Job completed")));

        mvc.perform(post("/api/recovery").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.command").value("impdp system/******@//localhost:1521/XEPDB1"))
                .andExpect(jsonPath("$.data.output[0]").value("Job completed"));

        verify(service).recover(new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", false));
    }

    @Test
    void failedRecoveryReturnsServerErrorWithOutput() throws Exception {
        when(service.recover(any())).thenReturn(
                new RecoveryResult(false, 1, "impdp system/******@//localhost:1521/XEPDB1", List.of("ORA-39002")));

        mvc.perform(post("/api/recovery").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.data.output[0]").value("ORA-39002"));
    }

    @Test
    void unmetPreconditionReturnsBadRequest() throws Exception {
        when(service.recover(any())).thenThrow(new IllegalArgumentException("Schema HR already exists"));

        mvc.perform(post("/api/recovery").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Schema HR already exists"));
    }

    @Test
    void missingFieldsAreRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/recovery").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
