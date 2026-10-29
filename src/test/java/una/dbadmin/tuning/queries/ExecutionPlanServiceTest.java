package una.dbadmin.tuning.queries;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

class ExecutionPlanServiceTest {
    @Test
    void explainsReadsAndCleansOnTheSameConnection() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        Statement explain = mock(Statement.class);
        PreparedStatement read = mock(PreparedStatement.class);
        PreparedStatement cleanup = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(explain);
        when(connection.prepareStatement(ExecutionPlanService.PLAN_SQL)).thenReturn(read);
        when(connection.prepareStatement("DELETE FROM plan_table WHERE statement_id = ?")).thenReturn(cleanup);
        when(read.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("operation")).thenReturn("TABLE ACCESS");
        when(rs.getString("options")).thenReturn("FULL");
        when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection));
        var rows = new ExecutionPlanService(jdbc).explain("SELECT * FROM HR.JOBS;");
        assertTrue(rows.get(0).fullTableScan());
        verify(explain).execute(matches("EXPLAIN PLAN SET STATEMENT_ID = '[a-f0-9]{30}' FOR SELECT \\* FROM HR.JOBS"));
        var order = inOrder(explain, read, cleanup);
        order.verify(explain).execute(anyString());
        order.verify(read).executeQuery();
        order.verify(cleanup).executeUpdate();
        verify(connection, never()).close();
    }
    @Test
    void rejectsEmptyStatementsBeforeAcquiringAConnection() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> new ExecutionPlanService(jdbc).explain("  "));
        verifyNoInteractions(jdbc);
    }
    @Test
    void cleansPlanRowsWhenExplainFails() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        Statement command = mock(Statement.class);
        PreparedStatement cleanup = mock(PreparedStatement.class);
        when(connection.createStatement()).thenReturn(command);
        when(command.execute(anyString())).thenThrow(new SQLException("invalid SQL"));
        when(connection.prepareStatement(anyString())).thenReturn(cleanup);
        when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection));
        assertThrows(Exception.class, () -> new ExecutionPlanService(jdbc).explain("invalid SQL"));
        verify(cleanup).executeUpdate();
    }
}
