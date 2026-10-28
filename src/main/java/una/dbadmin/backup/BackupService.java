package una.dbadmin.backup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs Data Pump exports.
 */
public class BackupService {

    /** {@code expdp} exits with 0 on success and 5 when it completed with warnings. */
    private static final int EXIT_SUCCESS_WITH_WARNINGS = 5;

    private static final String MASK = "******";

    private final ExpdpCommandBuilder builder;
    private final ProcessRunner runner;
    private final String password;

    public BackupService(ExpdpCommandBuilder builder, ProcessRunner runner, String password) {
        this.builder = builder;
        this.runner = runner;
        this.password = password;
    }

    /**
     * @throws IllegalArgumentException if the request contains an invalid name; nothing is executed in that case
     */
    public BackupResult backup(BackupRequest request) {
        DataPumpCommand command = builder.build(request);
        ProcessResult result = runWithParfile(command);
        boolean success = result.exitCode() == 0 || result.exitCode() == EXIT_SUCCESS_WITH_WARNINGS;
        return new BackupResult(success, result.exitCode(), command.display(), mask(result.output()));
    }

    /** The parameter file holds only table filters, never credentials, and is removed whatever the outcome. */
    private ProcessResult runWithParfile(DataPumpCommand command) {
        if (command.parfileLines().isEmpty()) {
            return runner.run(command.args());
        }
        Path parfile = null;
        try {
            parfile = Files.createTempFile("expdp-", ".par");
            Files.write(parfile, command.parfileLines());
            List<String> args = new ArrayList<>(command.args());
            args.add("PARFILE=" + parfile);
            return runner.run(args);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the Data Pump parameter file", e);
        } finally {
            deleteQuietly(parfile);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // The file only lists table names; failing to remove it must not hide the backup outcome.
        }
    }

    private List<String> mask(List<String> lines) {
        if (password == null || password.isEmpty()) {
            return lines;
        }
        return lines.stream().map(line -> line.replace(password, MASK)).toList();
    }
}
