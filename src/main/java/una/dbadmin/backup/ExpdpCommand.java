package una.dbadmin.backup;

import java.util.List;

/**
 * A ready-to-run {@code expdp} invocation.
 *
 * @param args    process arguments, including the real credentials
 * @param display the same command with the password masked, safe to log and show to the user
 */
public record ExpdpCommand(List<String> args, String display) {
}
