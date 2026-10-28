package una.dbadmin.backup;

import java.util.List;

/**
 * Outcome of a backup.
 *
 * @param success  {@code true} when the export finished, with or without warnings
 * @param exitCode exit code of {@code expdp}
 * @param command  the executed command with the password masked
 * @param output   console output of {@code expdp} with the password masked
 */
public record BackupResult(boolean success, int exitCode, String command, List<String> output) {
}
