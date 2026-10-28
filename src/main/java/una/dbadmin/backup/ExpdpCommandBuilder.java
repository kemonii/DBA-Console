package una.dbadmin.backup;

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
                request.type(), request.schema(), request.table(), request.directory(), request.fileName());
    }
}
