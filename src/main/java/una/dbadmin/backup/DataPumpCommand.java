package una.dbadmin.backup;

import java.util.List;

/**
 * A ready-to-run Data Pump ({@code expdp} or {@code impdp}) invocation.
 *
 * @param args    process arguments, including the real credentials
 * @param display the same command with the password masked, safe to log and show to the user
 */
public record DataPumpCommand(List<String> args, String display) {
}
