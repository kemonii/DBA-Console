package una.dbadmin.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input of a recovery operation.
 *
 * @param type        scope of the import
 * @param schema      schema owner; required for {@code TABLE} and {@code SCHEMA}
 * @param table       table name; required for {@code TABLE}
 * @param directory   name of the Oracle directory object that holds the dump file
 * @param fileName    base name of the dump file, without extension
 * @param confirmFull must be {@code true} to run a {@code FULL} import, which overwrites the whole database
 */
public record RecoveryRequest(
        @NotNull BackupType type,
        String schema,
        String table,
        @NotBlank String directory,
        @NotBlank String fileName,
        boolean confirmFull) {
}
