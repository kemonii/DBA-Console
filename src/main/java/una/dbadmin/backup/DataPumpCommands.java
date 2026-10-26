package una.dbadmin.backup;

import java.util.ArrayList;
import java.util.List;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Shared construction of {@code expdp} and {@code impdp} invocations. Every name that ends up in the command line goes
 * through {@link OracleIdentifiers#requireValid}, since it cannot be passed as a bind parameter.
 */
final class DataPumpCommands {

    private DataPumpCommands() {
    }

    /**
     * @param tool        {@code expdp} or {@code impdp}
     * @param operation   word used in validation messages, e.g. {@code backup}
     * @param logSuffix   appended to the file name to form the log file, e.g. {@code .log}
     * @param credentials {@code user/password@connectIdentifier}
     * @param masked      the same credentials with the password hidden
     */
    static DataPumpCommand build(String tool, String operation, String logSuffix, String credentials, String masked,
                                 BackupType type, String schema, String table, String directory, String fileName) {
        if (type == null) {
            throw new IllegalArgumentException("Type is required for a " + operation);
        }
        String validDirectory = OracleIdentifiers.requireValid(directory);
        OracleIdentifiers.requireValid(fileName);

        List<String> args = new ArrayList<>();
        args.add(tool);
        args.add(credentials);
        args.add(scopeParameter(type, schema, table, operation));
        args.add("DIRECTORY=" + validDirectory);
        args.add("DUMPFILE=" + fileName + ".dmp");
        args.add("LOGFILE=" + fileName + logSuffix);

        List<String> display = new ArrayList<>(args);
        display.set(1, masked);
        return new DataPumpCommand(List.copyOf(args), String.join(" ", display));
    }

    private static String scopeParameter(BackupType type, String schema, String table, String operation) {
        return switch (type) {
            case TABLE -> {
                requirePresent(schema, "Schema is required for a table " + operation);
                requirePresent(table, "Table is required for a table " + operation);
                yield "TABLES=" + OracleIdentifiers.requireValid(schema) + "." + OracleIdentifiers.requireValid(table);
            }
            case SCHEMA -> {
                requirePresent(schema, "Schema is required for a schema " + operation);
                yield "SCHEMAS=" + OracleIdentifiers.requireValid(schema);
            }
            case FULL -> "FULL=Y";
        };
    }

    private static void requirePresent(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
