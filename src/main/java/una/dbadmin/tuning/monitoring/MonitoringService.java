package una.dbadmin.tuning.monitoring;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
    private final JdbcTemplate jdbc;
    public MonitoringService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public List<Map<String, Object>> instance() {
        return jdbc.queryForList("SELECT instance_name, host_name, version, startup_time, status, database_status FROM v$instance");
    }
    public List<Map<String, Object>> parameters() {
        return jdbc.queryForList("SELECT name, value, isdefault, issys_modifiable FROM v$system_parameter WHERE name IN ('db_name', 'processes', 'sessions', 'open_cursors', 'sga_target', 'pga_aggregate_target', 'memory_target', 'optimizer_mode', 'statistics_level') ORDER BY name");
    }
    public List<Map<String, Object>> sessions() {
        return jdbc.queryForList("""
                SELECT s.sid, s.serial#, s.username, s.status, s.program, s.machine, s.type, s.sql_id,
                       s.event, s.blocking_session, NVL(c.value, 0) open_cursors
                FROM v$session s
                LEFT JOIN (SELECT ss.sid, ss.value FROM v$sesstat ss JOIN v$statname sn
                           ON sn.statistic# = ss.statistic# WHERE sn.name = 'opened cursors current') c ON c.sid = s.sid
                ORDER BY s.sid
                """);
    }
    public List<Map<String, Object>> sessionsByProgram() {
        return jdbc.queryForList("SELECT program, COUNT(*) session_count FROM v$session GROUP BY program ORDER BY session_count DESC");
    }
    public List<Map<String, Object>> sessionsByUser() {
        return jdbc.queryForList("SELECT username, COUNT(*) session_count FROM v$session GROUP BY username ORDER BY session_count DESC");
    }
    static final String BLOCKERS = """
            SELECT DISTINCT b.sid blocker_sid, b.serial# blocker_serial, b.username blocker_user,
                   w.sid waiting_sid, w.serial# waiting_serial, w.username waiting_user,
                   held.type lock_type, held.id1, held.id2, held.lmode, wanted.request
            FROM v$lock held JOIN v$lock wanted ON held.id1 = wanted.id1 AND held.id2 = wanted.id2 AND held.type = wanted.type
            JOIN v$session b ON b.sid = held.sid JOIN v$session w ON w.sid = wanted.sid
            WHERE held.lmode > 0 AND wanted.request > 0 AND held.sid <> wanted.sid AND held.block <> 0
            """;
    public List<Map<String, Object>> blockers() {
        return jdbc.queryForList(BLOCKERS);
    }
    public List<Map<String, Object>> lockedObjects() {
        return jdbc.queryForList("""
                SELECT s.sid, s.serial#, s.username, o.owner, o.object_name, o.object_type, l.locked_mode
                FROM v$locked_object l JOIN dba_objects o ON o.object_id = l.object_id
                JOIN v$session s ON s.sid = l.session_id ORDER BY s.sid, o.owner, o.object_name
                """);
    }
    public void killBlocker(int sid, int serial, boolean confirm) {
        if (!confirm || sid <= 0 || serial < 0) {
            throw new IllegalArgumentException("Confirm killing a valid blocking session with confirm=true");
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM (" + BLOCKERS + ") WHERE blocker_sid = ? AND blocker_serial = ?", Integer.class, sid, serial);
        if (count == null || count == 0) throw new IllegalArgumentException("Session is no longer blocking another session");
        jdbc.execute("ALTER SYSTEM KILL SESSION '" + sid + "," + serial + "' IMMEDIATE");
    }
}
