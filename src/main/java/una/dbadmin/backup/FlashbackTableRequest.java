package una.dbadmin.backup;

import jakarta.validation.constraints.NotBlank;

/**
 * Identifies a table to bring back from the recycle bin.
 */
public record FlashbackTableRequest(@NotBlank String schema, @NotBlank String table) {
}
