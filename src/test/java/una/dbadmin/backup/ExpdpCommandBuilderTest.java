package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExpdpCommandBuilderTest {

    private static final String CONNECT = "//localhost:1521/XEPDB1";

    private final ExpdpCommandBuilder builder = new ExpdpCommandBuilder("system", "s3cret", CONNECT);

    @Test
    void tableBackupUsesTablesParameter() {
        ExpdpCommand command = builder.build(new BackupRequest(BackupType.TABLE, "hr", "employees", "respaldo", "emp"));

        assertEquals(List.of("expdp", "system/s3cret@" + CONNECT, "TABLES=HR.EMPLOYEES", "DIRECTORY=RESPALDO",
                "DUMPFILE=emp.dmp", "LOGFILE=emp.log"), command.args());
    }

    @Test
    void schemaBackupUsesSchemasParameter() {
        ExpdpCommand command = builder.build(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr_full"));

        assertTrue(command.args().contains("SCHEMAS=HR"));
        assertTrue(command.args().contains("DUMPFILE=hr_full.dmp"));
        assertTrue(command.args().contains("LOGFILE=hr_full.log"));
    }

    @Test
    void fullBackupUsesFullParameter() {
        ExpdpCommand command = builder.build(new BackupRequest(BackupType.FULL, null, null, "respaldo", "everything"));

        assertTrue(command.args().contains("FULL=Y"));
        assertFalse(command.args().stream().anyMatch(a -> a.startsWith("TABLES=") || a.startsWith("SCHEMAS=")));
    }

    @Test
    void displayCommandHidesThePassword() {
        ExpdpCommand command = builder.build(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr"));

        assertFalse(command.display().contains("s3cret"));
        assertTrue(command.display().startsWith("expdp system/******@" + CONNECT));
    }

    @Test
    void tableBackupRequiresSchemaAndTable() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.TABLE, "hr", null, "respaldo", "emp")));
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.TABLE, null, "employees", "respaldo", "emp")));
    }

    @Test
    void schemaBackupRequiresSchema() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.SCHEMA, null, null, "respaldo", "hr")));
    }

    @Test
    void rejectsInvalidIdentifiersAndFileNames() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.SCHEMA, "hr; drop user x", null, "respaldo", "hr")));
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.SCHEMA, "hr", null, "resp aldo", "hr")));
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "../hr")));
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr.dmp")));
    }

    @Test
    void rejectsMissingType() {
        assertThrows(IllegalArgumentException.class,
                () -> builder.build(new BackupRequest(null, "hr", null, "respaldo", "hr")));
    }
}
