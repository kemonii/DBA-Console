package una.dbadmin.tuning.queries;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Service;
import una.dbadmin.common.OracleIdentifiers;

@Service
public class IndexService {
    public enum IndexType { BTREE, BITMAP }
    private final JdbcTemplate jdbc;
    public IndexService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String schema) {
        return jdbc.queryForList("SELECT owner, index_name, table_owner, table_name, index_type, status FROM dba_indexes WHERE owner = ? ORDER BY index_name",
                OracleIdentifiers.requireValid(schema));
    }
    public void create(String schema, String table, String index, List<String> columns, IndexType type) {
        String owner = OracleIdentifiers.requireValid(schema);
        String tableName = OracleIdentifiers.requireValid(table);
        String indexName = OracleIdentifiers.requireValid(index);
        if (type == null || columns == null || columns.isEmpty() || columns.size() > 32) {
            throw new IllegalArgumentException("Elija un tipo de índice y entre 1 y 32 columnas");
        }
        String names = String.join(", ", columns.stream().map(OracleIdentifiers::requireValid).toList());
        jdbc.execute("CREATE " + (type == IndexType.BITMAP ? "BITMAP " : "") + "INDEX " + owner + "." + indexName
                + " ON " + owner + "." + tableName + " (" + names + ")");
    }
    public void rebuild(String schema, String index) {
        jdbc.execute("ALTER INDEX " + qualified(schema, index) + " REBUILD");
    }
    public void drop(String schema, String index, boolean confirm) {
        String name = qualified(schema, index);
        if (!confirm) throw new IllegalArgumentException("Confirme la eliminación del índice con confirm=true");
        jdbc.execute("DROP INDEX " + name);
    }
    public List<Map<String, Object>> validate(String schema, String index) {
        String name = qualified(schema, index);
        return jdbc.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
            try (var command = connection.createStatement()) {
                command.execute("ANALYZE INDEX " + name + " VALIDATE STRUCTURE");
                try (var rs = command.executeQuery("SELECT name, height, blocks, lf_rows, lf_blks, del_lf_rows, distinct_keys, pct_used FROM index_stats")) {
                    var rows = new java.util.ArrayList<Map<String, Object>>();
                    var mapper = new ColumnMapRowMapper();
                    while (rs.next()) rows.add(mapper.mapRow(rs, rows.size()));
                    return rows;
                }
            }
        });
    }
    /** Owner and index come from a DBA_INDEXES row, so they are quoted exactly as stored. */
    private String qualified(String schema, String index) {
        return OracleIdentifiers.quoteExisting(schema) + "." + OracleIdentifiers.quoteExisting(index);
    }
}
