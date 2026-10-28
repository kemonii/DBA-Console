package una.dbadmin.security.tablespaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TablespaceServiceTest {

    private static final String DATAFILE_DIR = "/u01/oradata/XEPDB1";

    private JdbcTemplate jdbc;
    private TablespaceService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new TablespaceService(jdbc, DATAFILE_DIR);
    }

    /** The path the service builds, written with the separator of the platform running the tests. */
    private static String inDatafileDir(String fileName) {
        return Path.of(DATAFILE_DIR).resolve(fileName).toString();
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

    // ---- create ------------------------------------------------------------------------------------------------

    @Test
    void createBuildsTheStatementFromClass() {
        String name = service.create("ventas", "ventas01.dbf", 2, 1, 1024);

        assertEquals("VENTAS", name);
        verify(jdbc).execute("CREATE TABLESPACE VENTAS DATAFILE '" + inDatafileDir("ventas01.dbf")
                + "' SIZE 2M AUTOEXTEND ON NEXT 1M MAXSIZE 1024M");
    }

    @Test
    void createWithoutMaximumSizeIsUnlimited() {
        service.create("ventas", "ventas01.dbf", 2, 1, null);

        verify(jdbc).execute("CREATE TABLESPACE VENTAS DATAFILE '" + inDatafileDir("ventas01.dbf")
                + "' SIZE 2M AUTOEXTEND ON NEXT 1M MAXSIZE UNLIMITED");
    }

    @Test
    void createRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.create("bad name", "a.dbf", 2, 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.create(null, "a.dbf", 2, 1, null));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createAcceptsOnlyPlainDatafileNames() {
        for (String fileName : new String[] {"../system01.dbf", "C:\\x\\a.dbf", "/tmp/a.dbf", "a.txt", "a",
                "it's.dbf", ".dbf", null}) {
            assertThrows(IllegalArgumentException.class, () -> service.create("VENTAS", fileName, 2, 1, null),
                    "accepted " + fileName);
        }
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createValidatesSizes() {
        assertThrows(IllegalArgumentException.class, () -> service.create("V", "v.dbf", null, 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.create("V", "v.dbf", 0, 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.create("V", "v.dbf", 2, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("V", "v.dbf", TablespaceService.MAX_SIZE_MB + 1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.create("V", "v.dbf", 10, 1, 5));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- datafiles ---------------------------------------------------------------------------------------------

    @Test
    void addDatafileBuildsTheStatementFromClass() {
        service.addDatafile("ventas", "ventas02.dbf", 256);

        verify(jdbc).execute("ALTER TABLESPACE VENTAS ADD DATAFILE '" + inDatafileDir("ventas02.dbf")
                + "' SIZE 256M");
    }

    @Test
    void resizeUsesThePathFromTheDataDictionary() {
        when(jdbc.queryForList(TablespaceService.DATAFILE_PATH_QUERY, String.class, 12, "VENTAS"))
                .thenReturn(List.of("C:\\oradata\\ventas01.dbf"));

        service.resizeDatafile("ventas", 12, 5);

        verify(jdbc).execute("ALTER DATABASE DATAFILE 'C:\\oradata\\ventas01.dbf' RESIZE 5M");
    }

    @Test
    void resizeRejectsAFileOfAnotherTablespace() {
        when(jdbc.queryForList(TablespaceService.DATAFILE_PATH_QUERY, String.class, 1, "VENTAS"))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.resizeDatafile("ventas", 1, 5));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- drop --------------------------------------------------------------------------------------------------

    @Test
    void dropIncludesContentsAndDatafiles() {
        service.drop("ventas", true);

        verify(jdbc).execute("DROP TABLESPACE VENTAS INCLUDING CONTENTS AND DATAFILES");
    }

    @Test
    void dropRequiresConfirmation() {
        assertThrows(IllegalArgumentException.class, () -> service.drop("ventas", false));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropRefusesSystemTablespaces() {
        assertThrows(IllegalArgumentException.class, () -> service.drop("system", true));
        assertThrows(IllegalArgumentException.class, () -> service.drop("SYSAUX", true));
        verify(jdbc, never()).execute(anyString());
    }
}
