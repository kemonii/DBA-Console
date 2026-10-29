package una.dbadmin.tuning.monitoring;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import una.dbadmin.common.GlobalExceptionHandler;

class MonitoringControllerTest {
    @Test
    void terminatingRequiresExplicitConfirmationAndKeepsSerial() throws Exception {
        MonitoringService service = mock(MonitoringService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new MonitoringController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        doThrow(new IllegalArgumentException("Confirm termination")).when(service).killBlocker(42, 123, false);
        mvc.perform(delete("/api/tuning/monitoring/sessions/42/123"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.ok").value(false));
        mvc.perform(delete("/api/tuning/monitoring/sessions/42/123?confirm=true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        verify(service).killBlocker(42, 123, true);
    }
}
