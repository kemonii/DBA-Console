package una.dbadmin.backup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link ProcessRunner} backed by {@link ProcessBuilder}. Arguments are passed as a list, never through a shell.
 *
 * <p>The output is read on its own thread: reading only ends when the process closes its output, so reading it here
 * would block before the timeout is ever checked.
 */
public class SystemProcessRunner implements ProcessRunner {

    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    /** Once the process has exited, how long to wait for the last lines of output. */
    private static final long OUTPUT_GRACE_MILLIS = 5_000;

    private final Duration timeout;

    public SystemProcessRunner() {
        this(DEFAULT_TIMEOUT);
    }

    SystemProcessRunner(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public ProcessResult run(List<String> args) {
        String program = args.get(0);
        Process process;
        try {
            process = new ProcessBuilder(args).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo ejecutar " + program + "; verifique que esté instalado y en el PATH", e);
        }

        List<String> output = Collections.synchronizedList(new ArrayList<>());
        Thread reader = new Thread(() -> readLines(process, output), program + "-output");
        reader.setDaemon(true);
        reader.start();

        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        program + " no terminó en " + describe(timeout) + " y se detuvo");
            }
            reader.join(OUTPUT_GRACE_MILLIS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió mientras se ejecutaba " + program, e);
        }
        synchronized (output) {
            return new ProcessResult(process.exitValue(), List.copyOf(output));
        }
    }

    private static void readLines(Process process, List<String> output) {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        } catch (IOException ignored) {
            // The stream closes when a timed-out process is destroyed; the lines read so far are kept.
        }
    }

    private static String describe(Duration duration) {
        return duration.toMinutes() >= 1 ? duration.toMinutes() + " minutos" : duration.toSeconds() + " segundos";
    }
}
