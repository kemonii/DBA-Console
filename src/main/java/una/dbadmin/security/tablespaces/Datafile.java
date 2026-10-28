package una.dbadmin.security.tablespaces;

/**
 * A datafile of a tablespace, as listed in {@code DBA_DATA_FILES}.
 *
 * @param fileId         file number, used to pick the file to resize
 * @param fileName       full path on the database host
 * @param sizeMb         current size in megabytes
 * @param autoextensible whether the file grows automatically
 * @param maxSizeMb      maximum size in megabytes when it autoextends, {@code 0} otherwise
 */
public record Datafile(int fileId, String fileName, double sizeMb, boolean autoextensible, double maxSizeMb) {
}
