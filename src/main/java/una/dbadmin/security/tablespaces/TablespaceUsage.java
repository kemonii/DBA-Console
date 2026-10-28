package una.dbadmin.security.tablespaces;

/**
 * Size and space usage of a permanent or undo tablespace, in megabytes.
 *
 * @param name         tablespace name
 * @param totalMb      allocated size of its datafiles
 * @param usedMb       allocated size minus free space
 * @param freeMb       free space inside the allocated size (not counting future autoextend growth)
 * @param usedPercent  {@code usedMb} as a percentage of {@code totalMb}
 * @param freePercent  {@code freeMb} as a percentage of {@code totalMb}
 */
public record TablespaceUsage(
        String name, double totalMb, double usedMb, double freeMb, double usedPercent, double freePercent) {
}
