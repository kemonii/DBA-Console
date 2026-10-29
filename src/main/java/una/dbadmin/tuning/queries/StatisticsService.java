package una.dbadmin.tuning.queries;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import una.dbadmin.common.OracleIdentifiers;

@Service
public class StatisticsService {
    private final JdbcTemplate jdbc;
    public StatisticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String schema) {
        return jdbc.queryForList("SELECT owner, table_name, num_rows, last_analyzed FROM dba_tables WHERE owner = ? ORDER BY table_name",
                OracleIdentifiers.requireValid(schema));
    }
    public void gatherSchema(String schema) {
        jdbc.update("BEGIN DBMS_STATS.GATHER_SCHEMA_STATS(ownname => ?); END;", OracleIdentifiers.requireValid(schema));
    }
    public void gatherTable(String schema, String table) {
        String owner = OracleIdentifiers.requireValid(schema);
        String name = OracleIdentifiers.requireValid(table);
        jdbc.update("BEGIN DBMS_STATS.GATHER_TABLE_STATS(ownname => ?, tabname => ?); END;", owner, name);
    }
}
