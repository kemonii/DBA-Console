package una.dbadmin.backup;

import java.util.ArrayList;
import java.util.List;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Builds {@code expdp} invocations. Every name that ends up in the command line goes through
 * {@link OracleIdentifiers#requireValid}, since it cannot be passed as a bind parameter.
 */
public class ExpdpCommandBuilder {

    private final String user;
    private final String password;
    private final String connectIdentifier;

    /**
     * @param connectIdentifier Easy Connect identifier of the pluggable database, e.g. {@code //localhost:1521/XEPDB1}
     */
    public ExpdpCommandBuilder(String user, String password, String connectIdentifier) {
        this.user = user;
        this.password = password;
        this.connectIdentifier = connectIdentifier;
    }

    public ExpdpCommand build(BackupRequest request) {
        if (request.type() == null) {
            throw new IllegalArgumentException("Backup type is required");
        }
        String directory = OracleIdentifiers.requireValid(request.directory());
        String fileName = requireFileName(request.fileName());

        List<String> args = new ArrayList<>();
        args.add("expdp");
        args.add(user + "/" + password + "@" + connectIdentifier);
        args.add(scopeParameter(request));
        args.add("DIRECTORY=" + directory);
        args.add("DUMPFILE=" + fileName + ".dmp");
        args.add("LOGFILE=" + fileName + ".log");

        List<String> display = new ArrayList<>(args);
        display.set(1, user + "/******@" + connectIdentifier);
        return new ExpdpCommand(List.copyOf(args), String.join(" ", display));
    }

    private static String scopeParameter(BackupRequest request) {
        return switch (request.type()) {
            case TABLE -> {
                requirePresent(request.schema(), "Schema is required for a table backup");
                requirePresent(request.table(), "Table is required for a table backup");
                yield "TABLES=" + OracleIdentifiers.requireValid(request.schema()) + "."
                        + OracleIdentifiers.requireValid(request.table());
            }
            case SCHEMA -> {
                requirePresent(request.schema(), "Schema is required for a schema backup");
                yield "SCHEMAS=" + OracleIdentifiers.requireValid(request.schema());
            }
            case FULL -> "FULL=Y";
        };
    }

    /** File names follow identifier rules (no dots, slashes or spaces); the extension is added by the builder. */
    private static String requireFileName(String fileName) {
        OracleIdentifiers.requireValid(fileName);
        return fileName;
    }

    private static void requirePresent(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
