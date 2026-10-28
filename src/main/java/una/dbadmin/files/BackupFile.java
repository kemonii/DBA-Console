package una.dbadmin.files;

import java.time.Instant;

/**
 * A {@code .dmp} or {@code .log} file stored in a directory object's path.
 *
 * @param name         file name, without any path
 * @param sizeBytes    size of the file in bytes
 * @param lastModified last modification time
 */
public record BackupFile(String name, long sizeBytes, Instant lastModified) {
}
