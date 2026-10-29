package una.dbadmin.tuning.queries;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.sql.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class IndexServiceTest {
    @Test
    void rejectsInjectedColumnsAndMissingConfirmationBeforeExecutingDDL() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        IndexService service = new IndexService(jdbc);
        assertThrows(IllegalArgumentException.class, () -> service.create("hr", "jobs", "idx", List.of("id) --"), IndexService.IndexType.BTREE));
        assertThrows(IllegalArgumentException.class, () -> service.drop("hr", "idx", false));
        assertThrows(IllegalArgumentException.class, () -> service.create("hr", "jobs", "idx", List.of(), IndexService.IndexType.BTREE));
        verifyNoInteractions(jdbc);
    }
    @Test
    void createsBothIndexTypesWithValidatedNames() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        IndexService service = new IndexService(jdbc);
        service.create("hr", "jobs", "idx", List.of("job_id"), IndexService.IndexType.BTREE);
        service.create("hr", "jobs", "bm_idx", List.of("min_salary", "max_salary"), IndexService.IndexType.BITMAP);
        verify(jdbc).execute("CREATE INDEX HR.IDX ON HR.JOBS (JOB_ID)");
        verify(jdbc).execute("CREATE BITMAP INDEX HR.BM_IDX ON HR.JOBS (MIN_SALARY, MAX_SALARY)");
    }
    @Test
    void validatesAndReadsIndexStatsOnSameConnection() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        Statement command = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(command);
        when(command.executeQuery(anyString())).thenReturn(rs);
        when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection));
        assertTrue(new IndexService(jdbc).validate("hr", "idx").isEmpty());
        var order = inOrder(command);
        order.verify(command).execute("ANALYZE INDEX HR.IDX VALIDATE STRUCTURE");
        order.verify(command).executeQuery(contains("FROM index_stats"));
        verify(connection, never()).close();
    }
}
