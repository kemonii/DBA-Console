package una.dbadmin.tuning.queries;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class StatisticsServiceTest {
    @Test
    void validatesAllNamesBeforeCallingOracle() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        StatisticsService service = new StatisticsService(jdbc);
        assertThrows(IllegalArgumentException.class, () -> service.gatherTable("hr", "jobs;drop"));
        assertThrows(IllegalArgumentException.class, () -> service.gatherSchema(null));
        assertThrows(IllegalArgumentException.class, () -> service.list("bad owner"));
        verifyNoInteractions(jdbc);
    }
    @Test
    void bindsSchemaAndTableNamesForStatistics() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new StatisticsService(jdbc).gatherTable("hr", "jobs");
        verify(jdbc).update("BEGIN DBMS_STATS.GATHER_TABLE_STATS(ownname => ?, tabname => ?); END;", "HR", "JOBS");
    }
}
