package una.dbadmin.tuning.queries;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Explains without executing the supplied SQL; session-local rows never leave this connection. */
@Service
public class ExecutionPlanService {
    private final JdbcTemplate jdbc;
    public ExecutionPlanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PlanStep(int id, Integer parentId, int depth, String operation, String options,
            String owner, String objectName, Long cost, Long cardinality, boolean fullTableScan) {}

    static final String PLAN_SQL = """
            SELECT id, parent_id, LEVEL - 1 depth, operation, options, object_owner, object_name, cost, cardinality
            FROM (SELECT * FROM plan_table WHERE statement_id = ?)
            START WITH id = 0
            CONNECT BY PRIOR id = parent_id
            ORDER SIBLINGS BY position, id
            """;

    public List<PlanStep> explain(String statement) {
        if (statement == null || statement.isBlank() || statement.length() > 100000) {
            throw new IllegalArgumentException("Enter a SQL statement of at most 100000 characters");
        }
        String sql = statement.strip();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).stripTrailing();
        final String query = sql;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        return jdbc.execute((ConnectionCallback<List<PlanStep>>) connection -> {
            try {
                try (var command = connection.createStatement()) {
                    command.execute("EXPLAIN PLAN SET STATEMENT_ID = '" + id + "' FOR " + query);
                }
                try (var command = connection.prepareStatement(PLAN_SQL)) {
                    command.setString(1, id);
                    try (var rs = command.executeQuery()) {
                        var rows = new java.util.ArrayList<PlanStep>();
                        while (rs.next()) {
                            String operation = rs.getString("operation");
                            String options = rs.getString("options");
                            rows.add(new PlanStep(rs.getInt("id"), rs.getObject("parent_id", Integer.class),
                                    rs.getInt("depth"), operation, options, rs.getString("object_owner"),
                                    rs.getString("object_name"), rs.getObject("cost", Long.class),
                                    rs.getObject("cardinality", Long.class),
                                    "TABLE ACCESS".equals(operation) && "FULL".equals(options)));
                        }
                        return rows;
                    }
                }
            } finally {
                try (var cleanup = connection.prepareStatement("DELETE FROM plan_table WHERE statement_id = ?")) {
                    cleanup.setString(1, id);
                    cleanup.executeUpdate();
                }
            }
        });
    }
}
