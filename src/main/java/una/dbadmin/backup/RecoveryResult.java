package una.dbadmin.backup;

import java.util.List;

/**
 * Outcome of a recovery.
 *
 * @param success  {@code true} when the import finished, with or without warnings
 * @param exitCode exit code of {@code impdp}
 * @param command  the executed command with the password masked
 * @param output   console output of {@code impdp} with the password masked
 */
public record RecoveryResult(boolean success, int exitCode, String command, List<String> output) {
}
