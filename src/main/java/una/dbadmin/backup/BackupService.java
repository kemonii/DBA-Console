package una.dbadmin.backup;

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
        ExpdpCommand command = builder.build(request);
        ProcessResult result = runner.run(command.args());
        boolean success = result.exitCode() == 0 || result.exitCode() == EXIT_SUCCESS_WITH_WARNINGS;
        return new BackupResult(success, result.exitCode(), command.display(), mask(result.output()));
    }

    private List<String> mask(List<String> lines) {
        if (password == null || password.isEmpty()) {
            return lines;
        }
        return lines.stream().map(line -> line.replace(password, MASK)).toList();
    }
}
