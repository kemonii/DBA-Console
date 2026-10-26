package una.dbadmin.backup;

import java.util.List;

/**
 * A ready-to-run Data Pump ({@code expdp} or {@code impdp}) invocation.
 *
 * @param args         process arguments, including the real credentials
 * @param display      the command with the password masked and the parameter file lines appended, safe to show
 * @param parfileLines parameters that must travel in a {@code PARFILE}, because their quotes cannot be passed safely
 *                     on a command line; empty when none
 */
public record DataPumpCommand(List<String> args, String display, List<String> parfileLines) {
}
