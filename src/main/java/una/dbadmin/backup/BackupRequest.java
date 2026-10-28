package una.dbadmin.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input of a backup operation.
 *
 * @param type      scope of the export
 * @param schema    schema owner; required for {@code TABLE} and {@code SCHEMA}
 * @param table     table name; required for {@code TABLE}
 * @param directory name of the Oracle directory object that receives the files
 * @param fileName  base name of the dump and log files, without extension
 */
public record BackupRequest(
        @NotNull BackupType type,
        String schema,
        String table,
        @NotBlank String directory,
        @NotBlank String fileName) {
}
