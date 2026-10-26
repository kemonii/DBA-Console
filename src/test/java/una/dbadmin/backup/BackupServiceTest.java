package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BackupServiceTest {

    private static final BackupRequest REQUEST = new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr");

    private final List<List<String>> executed = new ArrayList<>();
    private final ExpdpCommandBuilder builder = new ExpdpCommandBuilder("system", "s3cret", "//localhost:1521/XEPDB1");

    private BackupService serviceReturning(int exitCode, String... output) {
        ProcessRunner runner = args -> {
            executed.add(args);
            return new ProcessResult(exitCode, List.of(output));
        };
        return new BackupService(builder, runner, "s3cret");
    }

    @Test
    void runsTheBuiltCommand() {
        serviceReturning(0).backup(REQUEST);

        assertEquals(builder.build(REQUEST).args(), executed.get(0));
    }

    @Test
    void exitCodeZeroIsSuccess() {
        BackupResult result = serviceReturning(0, "Job completed").backup(REQUEST);

        assertTrue(result.success());
        assertEquals(0, result.exitCode());
        assertEquals(List.of("Job completed"), result.output());
    }

    @Test
    void exitCodeFiveMeansCompletedWithWarnings() {
        BackupResult result = serviceReturning(5).backup(REQUEST);

        assertTrue(result.success());
        assertEquals(5, result.exitCode());
    }

    @Test
    void otherExitCodesAreFailures() {
        BackupResult result = serviceReturning(1, "ORA-39002: invalid operation").backup(REQUEST);

        assertFalse(result.success());
        assertEquals(List.of("ORA-39002: invalid operation"), result.output());
    }

    @Test
    void resultShowsTheCommandWithoutPassword() {
        BackupResult result = serviceReturning(0, "connected as system/s3cret@//localhost:1521/XEPDB1").backup(REQUEST);

        assertTrue(result.command().startsWith("expdp system/******@"));
        assertFalse(result.command().contains("s3cret"));
        assertFalse(String.join("\n", result.output()).contains("s3cret"));
    }

    @Test
    void invalidRequestNeverReachesTheRunner() {
        BackupService service = serviceReturning(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.backup(new BackupRequest(BackupType.SCHEMA, "hr; drop user x", null, "respaldo", "hr")));
        assertTrue(executed.isEmpty());
    }
}
