package una.dbadmin.backup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

class BackupLogControllerTest {

    private final BackupLogService service = mock(BackupLogService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BackupLogController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsTheLogLines() throws Exception {
        when(service.read("respaldo", "emp.log")).thenReturn(List.of("Export started", "Job completed"));

        mvc.perform(get("/api/backup/log").param("directory", "respaldo").param("fileName", "emp.log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data[0]").value("Export started"))
                .andExpect(jsonPath("$.data[1]").value("Job completed"));
    }

    @Test
    void invalidOrMissingFileIsABadRequest() throws Exception {
        when(service.read("respaldo", "x.log"))
                .thenThrow(new IllegalArgumentException("Log file x.log not found in directory RESPALDO"));

        mvc.perform(get("/api/backup/log").param("directory", "respaldo").param("fileName", "x.log"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Log file x.log not found in directory RESPALDO"));
    }

    @Test
    void missingParametersAreRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(get("/api/backup/log").param("directory", "respaldo")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
