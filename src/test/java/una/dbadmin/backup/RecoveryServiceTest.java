package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecoveryServiceTest {

    private static final RecoveryRequest TABLE =
            new RecoveryRequest(BackupType.TABLE, "hr", "employees", "respaldo", "emp", false);
    private static final RecoveryRequest SCHEMA =
            new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", false);

    private final List<List<String>> executed = new ArrayList<>();
    private final OracleCatalog catalog = mock(OracleCatalog.class);
    private final ImpdpCommandBuilder builder = new ImpdpCommandBuilder("system", "s3cret", "//localhost:1521/XEPDB1");

    private RecoveryService serviceReturning(int exitCode, String... output) {
        ProcessRunner runner = args -> {
            executed.add(args);
            return new ProcessResult(exitCode, List.of(output));
        };
        return new RecoveryService(builder, runner, catalog, "s3cret");
    }

    @Test
    void recoversATableThatWasDropped() {
        when(catalog.tableExists("HR", "EMPLOYEES")).thenReturn(false);

        RecoveryResult result = serviceReturning(0, "Job completed").recover(TABLE);

        assertTrue(result.success());
        assertEquals(builder.build(TABLE).args(), executed.get(0));
    }

    @Test
    void refusesToRecoverATableThatStillExists() {
        when(catalog.tableExists("HR", "EMPLOYEES")).thenReturn(true);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> serviceReturning(0).recover(TABLE));

        assertTrue(error.getMessage().contains("HR.EMPLOYEES"));
        assertTrue(executed.isEmpty());
    }

    @Test
    void recoversASchemaThatDoesNotExist() {
        when(catalog.userExists("HR")).thenReturn(false);

        assertTrue(serviceReturning(0).recover(SCHEMA).success());
        assertEquals(1, executed.size());
    }

    @Test
    void refusesToRecoverASchemaThatStillExists() {
        when(catalog.userExists("HR")).thenReturn(true);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> serviceReturning(0).recover(SCHEMA));

        assertTrue(error.getMessage().contains("HR"));
        assertTrue(executed.isEmpty());
    }

    @Test
    void fullRecoveryNeedsExplicitConfirmation() {
        RecoveryRequest unconfirmed = new RecoveryRequest(BackupType.FULL, null, null, "respaldo", "everything", false);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> serviceReturning(0).recover(unconfirmed));

        assertTrue(error.getMessage().contains("confirmFull"));
        assertTrue(executed.isEmpty());
    }

    @Test
    void confirmedFullRecoveryRuns() {
        RecoveryRequest confirmed = new RecoveryRequest(BackupType.FULL, null, null, "respaldo", "everything", true);

        assertTrue(serviceReturning(0).recover(confirmed).success());
        assertEquals(1, executed.size());
    }

    @Test
    void exitCodeFiveIsSuccessAndOtherCodesFail() {
        assertTrue(serviceReturning(5).recover(SCHEMA).success());
        assertFalse(serviceReturning(1, "ORA-39002").recover(SCHEMA).success());
    }

    @Test
    void resultNeverContainsThePassword() {
        RecoveryResult result = serviceReturning(0, "connected as system/s3cret@//localhost:1521/XEPDB1").recover(SCHEMA);

        assertFalse(result.command().contains("s3cret"));
        assertFalse(String.join("\n", result.output()).contains("s3cret"));
    }

    @Test
    void invalidNamesNeverReachTheCatalogOrTheRunner() {
        RecoveryRequest invalid = new RecoveryRequest(BackupType.SCHEMA, "hr; drop user x", null, "respaldo", "hr", false);

        assertThrows(IllegalArgumentException.class, () -> serviceReturning(0).recover(invalid));
        assertTrue(executed.isEmpty());
    }
}
