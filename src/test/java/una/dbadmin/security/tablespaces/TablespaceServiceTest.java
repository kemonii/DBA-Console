package una.dbadmin.security.tablespaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TablespaceServiceTest {

    private JdbcTemplate jdbc;
    private TablespaceService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new TablespaceService(jdbc);
    }

    // ---- listing -----------------------------------------------------------------------------------------------

    @Test
    void listUsageMapsTheClassScriptColumns() {
        when(jdbc.queryForList(TablespaceService.USAGE_QUERY)).thenReturn(List.of(Map.of(
                "TABLESPACE_NAME", "USERS",
                "TOTAL_MB", new BigDecimal("5"),
                "USED_MB", new BigDecimal("1.25"),
                "FREE_MB", new BigDecimal("3.75"),
                "USED_PERCENT", new BigDecimal("25"),
                "FREE_PERCENT", new BigDecimal("75"))));

        assertEquals(List.of(new TablespaceUsage("USERS", 5, 1.25, 3.75, 25, 75)), service.listUsage());
    }

    @Test
    void listDatafilesConvertsBytesToMegabytes() {
        when(jdbc.queryForList(TablespaceService.DATAFILES_QUERY, "USERS")).thenReturn(List.of(Map.of(
                "FILE_ID", new BigDecimal("12"),
                "FILE_NAME", "/u01/oradata/XEPDB1/users01.dbf",
                "BYTES", new BigDecimal(5L * 1024 * 1024),
                "AUTOEXTENSIBLE", "YES",
                "MAXBYTES", new BigDecimal(1024L * 1024 * 1024))));

        List<Datafile> result = service.listDatafiles("users");

        assertEquals(List.of(new Datafile(12, "/u01/oradata/XEPDB1/users01.dbf", 5, true, 1024)), result);
    }

    @Test
    void listDatafilesRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.listDatafiles("x' OR '1'='1"));
    }
}
