package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BackupServiceParfileTest {

    private static final BackupRequest FILTERED = new BackupRequest(
            BackupType.SCHEMA, "hr", null, "respaldo", "hr", List.of("employees"), null);

    private final ExpdpCommandBuilder builder = new ExpdpCommandBuilder("system", "s3cret", "//localhost:1521/XEPDB1");

    private final List<String> parfileContent = new ArrayList<>();
    private Path parfilePath;

    private BackupService service(ProcessRunner runner) {
        return new BackupService(builder, runner, "s3cret");
    }

    /** Captures the parameter file while the process "runs", since the service deletes it afterwards. */
    private ProcessRunner capturingRunner() {
        return args -> {
            String parfileArg = args.stream().filter(a -> a.startsWith("PARFILE=")).findFirst().orElseThrow();
            parfilePath = Path.of(parfileArg.substring("PARFILE=".length()));
            try {
                parfileContent.addAll(Files.readAllLines(parfilePath));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new ProcessResult(0, List.of());
        };
    }

    @Test
    void passesFiltersToExpdpThroughAParameterFile() {
        service(capturingRunner()).backup(FILTERED);

        assertEquals(List.of("INCLUDE=TABLE:\"IN ('EMPLOYEES')\""), parfileContent);
    }

    @Test
    void deletesTheParameterFileAfterTheRun() {
        service(capturingRunner()).backup(FILTERED);

        assertFalse(Files.exists(parfilePath));
    }

    @Test
    void deletesTheParameterFileEvenWhenTheProcessFails() {
        ProcessRunner failing = args -> {
            parfilePath = Path.of(args.get(args.size() - 1).substring("PARFILE=".length()));
            throw new IllegalStateException("boom");
        };

        assertThrows(IllegalStateException.class, () -> service(failing).backup(FILTERED));

        assertFalse(Files.exists(parfilePath));
    }

    @Test
    void withoutFiltersNoParameterFileIsUsed() {
        List<List<String>> executed = new ArrayList<>();
        ProcessRunner runner = args -> {
            executed.add(args);
            return new ProcessResult(0, List.of());
        };

        service(runner).backup(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr"));

        assertTrue(executed.get(0).stream().noneMatch(a -> a.startsWith("PARFILE=")));
    }
}
