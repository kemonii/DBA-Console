package una.dbadmin.tuning.queries;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import una.dbadmin.common.GlobalExceptionHandler;

class QueryTuningControllerTest {
    private ExecutionPlanService plans;
    private StatisticsService statistics;
    private IndexService indexes;
    private MockMvc mvc;
    @BeforeEach
    void setup() {
        plans = mock(ExecutionPlanService.class);
        statistics = mock(StatisticsService.class);
        indexes = mock(IndexService.class);
        mvc = MockMvcBuilders.standaloneSetup(new QueryTuningController(plans, statistics, indexes))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test
    void returnsPlanInOperationResult() throws Exception {
        when(plans.explain("SELECT * FROM HR.JOBS")).thenReturn(List.of(
                new ExecutionPlanService.PlanStep(1, 0, 1, "TABLE ACCESS", "FULL", "HR", "JOBS", 5L, 19L, true)));
        mvc.perform(post("/api/tuning/queries/plan").contentType(MediaType.APPLICATION_JSON)
                .content("{\"statement\":\"SELECT * FROM HR.JOBS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data[0].fullTableScan").value(true));
    }
    @Test
    void invalidIdentifiersUseStandardBadRequestEnvelope() throws Exception {
        when(statistics.list("bad name")).thenThrow(new IllegalArgumentException("Invalid Oracle identifier"));
        mvc.perform(get("/api/tuning/queries/statistics/bad name"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.ok").value(false));
    }
    @Test
    void droppingAnIndexDefaultsToUnconfirmed() throws Exception {
        doThrow(new IllegalArgumentException("Confirm dropping the index")).when(indexes).drop("HR", "IDX", false);
        mvc.perform(delete("/api/tuning/queries/indexes/HR/IDX"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.ok").value(false));
        mvc.perform(delete("/api/tuning/queries/indexes/HR/IDX?confirm=true"))
                .andExpect(status().isOk());
        verify(indexes).drop("HR", "IDX", true);
    }
    @Test
    void routesSchemaAndTableGatherSeparately() throws Exception {
        mvc.perform(post("/api/tuning/queries/statistics/HR/gather")).andExpect(status().isOk());
        mvc.perform(post("/api/tuning/queries/statistics/HR/JOBS/gather")).andExpect(status().isOk());
        verify(statistics).gatherSchema("HR");
        verify(statistics).gatherTable("HR", "JOBS");
    }
}
