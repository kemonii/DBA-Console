package una.dbadmin.backup;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Identifies a table and the point in time to look at.
 *
 * @param timestamp moment in the past, in the database time zone, e.g. {@code 2026-10-26T10:15:00}
 */
public record FlashbackQueryRequest(@NotBlank String schema, @NotBlank String table, @NotNull LocalDateTime timestamp) {
}
