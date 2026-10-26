package una.dbadmin.backup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import una.dbadmin.common.GlobalExceptionHandler;

class FlashbackControllerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 26, 10, 15, 0);
    private static final String TABLE_BODY = """
            {"schema":"hr","table":"employees"}""";
    private static final String AT_BODY = """
            {"schema":"hr","table":"employees","timestamp":"2026-10-26T10:15:00"}""";

    private final FlashbackService service = mock(FlashbackService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FlashbackController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void restoreTableReturnsOk() throws Exception {
        mvc.perform(post("/api/flashback/table").contentType(MediaType.APPLICATION_JSON).content(TABLE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.message").value("Table HR.EMPLOYEES restored from the recycle bin"));

        verify(service).restoreTable(new FlashbackTableRequest("hr", "employees"));
    }

    @Test
    void restoreTableReportsAMissingRecycleBinEntryAsBadRequest() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Table HR.EMPLOYEES is not in the recycle bin"))
                .when(service).restoreTable(new FlashbackTableRequest("hr", "employees"));

        mvc.perform(post("/api/flashback/table").contentType(MediaType.APPLICATION_JSON).content(TABLE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value("Table HR.EMPLOYEES is not in the recycle bin"));
    }

    @Test
    void queryReturnsTheRowsAsOfTheTimestamp() throws Exception {
        when(service.queryAsOf(new FlashbackQueryRequest("hr", "employees", AT)))
                .thenReturn(List.of(Map.of("ID", 1)));

        mvc.perform(post("/api/flashback/query").contentType(MediaType.APPLICATION_JSON).content(AT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ID").value(1));
    }

    @Test
    void restoreRowsReportsHowManyWereInserted() throws Exception {
        when(service.restoreRows(new FlashbackQueryRequest("hr", "employees", AT))).thenReturn(3);

        mvc.perform(post("/api/flashback/restore").contentType(MediaType.APPLICATION_JSON).content(AT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("3 row(s) restored"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void missingFieldsAreRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/flashback/query").contentType(MediaType.APPLICATION_JSON).content(TABLE_BODY))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/flashback/table").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
