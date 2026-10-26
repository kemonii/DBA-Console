package una.dbadmin.backup;

/**
 * Input of a backup operation.
 *
 * @param type      scope of the export
 * @param schema    schema owner; required for {@code TABLE} and {@code SCHEMA}
 * @param table     table name; required for {@code TABLE}
 * @param directory name of the Oracle directory object that receives the files
 * @param fileName  base name of the dump and log files, without extension
 */
public record BackupRequest(BackupType type, String schema, String table, String directory, String fileName) {
}
