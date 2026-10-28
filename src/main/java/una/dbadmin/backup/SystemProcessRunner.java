package una.dbadmin.backup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link ProcessRunner} backed by {@link ProcessBuilder}. Arguments are passed as a list, never through a shell.
 */
public class SystemProcessRunner implements ProcessRunner {

    private static final long TIMEOUT_MINUTES = 30;

    @Override
    public ProcessResult run(List<String> args) {
        try {
            Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
            List<String> output;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().toList();
            }
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IllegalStateException("Process timed out after " + TIMEOUT_MINUTES + " minutes");
            }
            return new ProcessResult(process.exitValue(), output);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not run " + args.get(0), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running " + args.get(0), e);
        }
    }
}
