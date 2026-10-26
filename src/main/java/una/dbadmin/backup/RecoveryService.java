package una.dbadmin.backup;

import java.util.List;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Runs Data Pump imports after checking that the target is in the state the import needs. Nothing is dropped here:
 * a table or schema that still exists is reported back so the user removes it deliberately.
 */
public class RecoveryService {

    /** {@code impdp} exits with 0 on success and 5 when it completed with warnings. */
    private static final int EXIT_SUCCESS_WITH_WARNINGS = 5;

    private static final String MASK = "******";

    private final ImpdpCommandBuilder builder;
    private final ProcessRunner runner;
    private final OracleCatalog catalog;
    private final String password;

    public RecoveryService(ImpdpCommandBuilder builder, ProcessRunner runner, OracleCatalog catalog, String password) {
        this.builder = builder;
        this.runner = runner;
        this.catalog = catalog;
        this.password = password;
    }

    /**
     * @throws IllegalArgumentException if a name is invalid or a precondition is not met; nothing is executed then
     */
    public RecoveryResult recover(RecoveryRequest request) {
        DataPumpCommand command = builder.build(request);
        checkPreconditions(request);
        ProcessResult result = runner.run(command.args());
        boolean success = result.exitCode() == 0 || result.exitCode() == EXIT_SUCCESS_WITH_WARNINGS;
        return new RecoveryResult(success, result.exitCode(), command.display(), mask(result.output()));
    }

    private void checkPreconditions(RecoveryRequest request) {
        switch (request.type()) {
            case TABLE -> {
                String schema = OracleIdentifiers.requireValid(request.schema());
                String table = OracleIdentifiers.requireValid(request.table());
                if (catalog.tableExists(schema, table)) {
                    throw new IllegalArgumentException(
                            "Table " + schema + "." + table + " already exists; drop it before recovering it");
                }
            }
            case SCHEMA -> {
                String schema = OracleIdentifiers.requireValid(request.schema());
                if (catalog.userExists(schema)) {
                    throw new IllegalArgumentException(
                            "Schema " + schema + " already exists; drop the user before recovering it");
                }
            }
            case FULL -> {
                if (!request.confirmFull()) {
                    throw new IllegalArgumentException("Full recovery overwrites the whole database. "
                            + "Send confirmFull=true to run it");
                }
            }
        }
    }

    private List<String> mask(List<String> lines) {
        if (password == null || password.isEmpty()) {
            return lines;
        }
        return lines.stream().map(line -> line.replace(password, MASK)).toList();
    }
}
