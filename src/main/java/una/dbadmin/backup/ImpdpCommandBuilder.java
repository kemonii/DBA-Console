package una.dbadmin.backup;

/**
 * Builds {@code impdp} invocations. The log file gets an {@code _imp} suffix so it never overwrites the export log.
 */
public class ImpdpCommandBuilder {

    private final String user;
    private final String password;
    private final String connectIdentifier;

    /**
     * @param connectIdentifier Easy Connect identifier of the pluggable database, e.g. {@code //localhost:1521/XEPDB1}
     */
    public ImpdpCommandBuilder(String user, String password, String connectIdentifier) {
        this.user = user;
        this.password = password;
        this.connectIdentifier = connectIdentifier;
    }

    public DataPumpCommand build(RecoveryRequest request) {
        return DataPumpCommands.build("impdp", "recovery", "_imp.log",
                user + "/" + password + "@" + connectIdentifier, user + "/******@" + connectIdentifier,
                request.type(), request.schema(), request.table(), request.directory(), request.fileName());
    }
}
