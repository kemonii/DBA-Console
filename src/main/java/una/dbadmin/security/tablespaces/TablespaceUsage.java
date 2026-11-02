package una.dbadmin.security.tablespaces;

/**
 * Size and space usage of a tablespace, in megabytes.
 *
 * @param name         tablespace name
 * @param contents     kind of tablespace: PERMANENT, TEMPORARY or UNDO
 * @param totalMb      allocated size of its datafiles or tempfiles
 * @param usedMb       allocated size minus free space
 * @param freeMb       free space inside the allocated size (not counting future autoextend growth)
 * @param usedPercent  {@code usedMb} as a percentage of {@code totalMb}
 * @param freePercent  {@code freeMb} as a percentage of {@code totalMb}
 */
public record TablespaceUsage(
        String name, String contents, double totalMb, double usedMb, double freeMb, double usedPercent, double freePercent) {
}
