package una.dbadmin.tuning.monitoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MonitoringServiceTest {
    @Test
    void requiresConfirmationBeforeAnyDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> new MonitoringService(jdbc).killBlocker(10, 20, false));
        assertThrows(IllegalArgumentException.class, () -> new MonitoringService(jdbc).killBlocker(-1, 20, true));
        verifyNoInteractions(jdbc);
    }
    @Test
    void refusesAStaleOrReusedSessionAndKillsOnlyAnExactBlockingSession() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        String sql = "SELECT COUNT(*) FROM (" + MonitoringService.BLOCKERS + ") WHERE blocker_sid = ? AND blocker_serial = ?";
        when(jdbc.queryForObject(sql, Integer.class, 10, 20)).thenReturn(0, 1);
        MonitoringService service = new MonitoringService(jdbc);
        assertThrows(IllegalArgumentException.class, () -> service.killBlocker(10, 20, true));
        verify(jdbc, never()).execute(anyString());
        service.killBlocker(10, 20, true);
        verify(jdbc).execute("ALTER SYSTEM KILL SESSION '10,20' IMMEDIATE");
    }
}
