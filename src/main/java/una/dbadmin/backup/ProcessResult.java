package una.dbadmin.backup;

import java.util.List;

/**
 * Outcome of an OS process.
 *
 * @param exitCode process exit code
 * @param output   combined stdout and stderr, one entry per line
 */
public record ProcessResult(int exitCode, List<String> output) {
}
