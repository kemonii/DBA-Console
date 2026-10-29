package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs real processes: the Java launcher of the JVM running the tests, so it works on any OS. */
class SystemProcessRunnerTest {

    private static final String JAVA = ProcessHandle.current().info().command().orElse("java");

    @TempDir
    Path dir;

    @Test
    void returnsTheExitCodeAndTheCombinedOutput() {
        // java -version writes to stderr, which is merged into the output.
        ProcessResult result = new SystemProcessRunner().run(List.of(JAVA, "-version"));

        assertEquals(0, result.exitCode());
        assertFalse(result.output().isEmpty());
        assertTrue(String.join("\n", result.output()).contains("version"));
    }

    @Test
    void stopsAProcessThatDoesNotFinishInTime() throws IOException {
        Path sleeper = dir.resolve("Sleeper.java");
        Files.writeString(sleeper, """
                public class Sleeper {
                    public static void main(String[] args) throws Exception {
                        System.out.println("started");
                        Thread.sleep(60_000);
                    }
                }
                """);
        SystemProcessRunner runner = new SystemProcessRunner(Duration.ofSeconds(3));

        long start = System.nanoTime();
        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> runner.run(List.of(JAVA, sleeper.toString())));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertEquals(JAVA + " no terminó en 3 segundos y se detuvo", error.getMessage());
        assertTrue(elapsed.compareTo(Duration.ofSeconds(30)) < 0, "took " + elapsed);
    }

    @Test
    void explainsAProgramThatIsNotOnThePath() {
        UncheckedIOException error = assertThrows(UncheckedIOException.class,
                () -> new SystemProcessRunner().run(List.of("dba-console-no-such-program")));

        assertEquals("No se pudo ejecutar dba-console-no-such-program; verifique que esté instalado y en el PATH",
                error.getMessage());
    }
}
