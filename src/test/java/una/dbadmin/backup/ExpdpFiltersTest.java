package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExpdpFiltersTest {

    private final ExpdpCommandBuilder builder =
            new ExpdpCommandBuilder("system", "s3cret", "//localhost:1521/XEPDB1");

    private static BackupRequest schemaWith(List<String> include, List<String> exclude) {
        return new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", include, exclude);
    }

    @Test
    void includeTablesGoToTheParameterFileNotTheCommandLine() {
        DataPumpCommand command = builder.build(schemaWith(List.of("employees", "departments"), null));

        assertEquals(List.of("INCLUDE=TABLE:\"IN ('EMPLOYEES','DEPARTMENTS')\""), command.parfileLines());
        assertFalse(command.args().stream().anyMatch(a -> a.startsWith("INCLUDE=")));
    }

    @Test
    void excludeTablesUseTheExcludeParameter() {
        DataPumpCommand command = builder.build(schemaWith(null, List.of("audit_log")));

        assertEquals(List.of("EXCLUDE=TABLE:\"IN ('AUDIT_LOG')\""), command.parfileLines());
    }

    @Test
    void displayShowsTheFilterToTheUser() {
        DataPumpCommand command = builder.build(schemaWith(List.of("employees"), null));

        assertTrue(command.display().contains("INCLUDE=TABLE:\"IN ('EMPLOYEES')\""));
        assertFalse(command.display().contains("s3cret"));
    }

    @Test
    void noFiltersMeansNoParameterFile() {
        assertTrue(builder.build(schemaWith(null, null)).parfileLines().isEmpty());
        assertTrue(builder.build(schemaWith(List.of(), List.of())).parfileLines().isEmpty());
    }

    @Test
    void fullBackupsAcceptFilters() {
        BackupRequest request =
                new BackupRequest(BackupType.FULL, null, null, "respaldo", "all", null, List.of("audit_log"));

        assertEquals(1, builder.build(request).parfileLines().size());
    }

    @Test
    void includeAndExcludeCannotBeCombined() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(schemaWith(List.of("employees"), List.of("audit_log"))));
    }

    @Test
    void tableBackupsDoNotAcceptFilters() {
        BackupRequest request = new BackupRequest(
                BackupType.TABLE, "hr", "employees", "respaldo", "emp", List.of("employees"), null);

        assertThrows(IllegalArgumentException.class, () -> builder.build(request));
    }

    @Test
    void filterNamesAreValidated() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(schemaWith(List.of("employees') OR ('1'='1"), null)));
        assertThrows(IllegalArgumentException.class, () -> builder.build(schemaWith(null, List.of("a b"))));
    }
}
