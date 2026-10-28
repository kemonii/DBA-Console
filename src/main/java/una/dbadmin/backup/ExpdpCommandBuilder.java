package una.dbadmin.backup;

import java.util.List;
import java.util.stream.Collectors;

import una.dbadmin.common.OracleIdentifiers;

/**
 * Builds {@code expdp} invocations.
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

    public DataPumpCommand build(BackupRequest request) {
        return DataPumpCommands.build("expdp", "backup", ".log",
                user + "/" + password + "@" + connectIdentifier, user + "/******@" + connectIdentifier,
                request.type(), request.schema(), request.table(), request.directory(), request.fileName(),
                tableFilters(request));
    }

    /**
     * The filter is quoted ({@code INCLUDE=TABLE:"IN ('A','B')"}), and those quotes cannot be passed reliably as a
     * process argument on every platform, so it is returned as a parameter file line.
     */
    private static List<String> tableFilters(BackupRequest request) {
        boolean include = request.includeTables() != null && !request.includeTables().isEmpty();
        boolean exclude = request.excludeTables() != null && !request.excludeTables().isEmpty();
        if (!include && !exclude) {
            return List.of();
        }
        if (include && exclude) {
            throw new IllegalArgumentException("includeTables and excludeTables cannot be used together");
        }
        if (request.type() == BackupType.TABLE) {
            throw new IllegalArgumentException("Table filters apply to schema and full backups, not to a table backup");
        }
        return List.of(include
                ? filter("INCLUDE", request.includeTables())
                : filter("EXCLUDE", request.excludeTables()));
    }

    private static String filter(String parameter, List<String> tables) {
        String names = tables.stream()
                .map(name -> "'" + OracleIdentifiers.requireValid(name) + "'")
                .collect(Collectors.joining(","));
        return parameter + "=TABLE:\"IN (" + names + ")\"";
    }
}
