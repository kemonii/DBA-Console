package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ImpdpCommandBuilderTest {

    private static final String CONNECT = "//localhost:1521/XEPDB1";

    private final ImpdpCommandBuilder builder = new ImpdpCommandBuilder("system", "s3cret", CONNECT);

    @Test
    void tableRecoveryUsesTablesParameter() {
        DataPumpCommand command =
                builder.build(new RecoveryRequest(BackupType.TABLE, "hr", "employees", "respaldo", "emp", false));

        assertEquals(List.of("impdp", "system/s3cret@" + CONNECT, "TABLES=HR.EMPLOYEES", "DIRECTORY=RESPALDO",
                "DUMPFILE=emp.dmp", "LOGFILE=emp_imp.log"), command.args());
    }

    @Test
    void schemaRecoveryUsesSchemasParameter() {
        DataPumpCommand command =
                builder.build(new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", false));

        assertTrue(command.args().contains("SCHEMAS=HR"));
        assertTrue(command.args().contains("LOGFILE=hr_imp.log"));
    }

    @Test
    void fullRecoveryUsesFullParameter() {
        DataPumpCommand command =
                builder.build(new RecoveryRequest(BackupType.FULL, null, null, "respaldo", "everything", true));

        assertTrue(command.args().contains("FULL=Y"));
    }

    @Test
    void importLogDoesNotOverwriteTheExportLog() {
        DataPumpCommand command =
                builder.build(new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", false));

        assertFalse(command.args().contains("LOGFILE=hr.log"));
    }

    @Test
    void displayCommandHidesThePassword() {
        DataPumpCommand command =
                builder.build(new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "hr", false));

        assertFalse(command.display().contains("s3cret"));
        assertTrue(command.display().startsWith("impdp system/******@" + CONNECT));
    }

    @Test
    void rejectsInvalidNamesAndMissingParts() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(
                new RecoveryRequest(BackupType.SCHEMA, "hr; drop user x", null, "respaldo", "hr", false)));
        assertThrows(IllegalArgumentException.class, () -> builder.build(
                new RecoveryRequest(BackupType.SCHEMA, "hr", null, "respaldo", "../hr", false)));
        assertThrows(IllegalArgumentException.class, () -> builder.build(
                new RecoveryRequest(BackupType.TABLE, "hr", null, "respaldo", "emp", false)));
        assertThrows(IllegalArgumentException.class, () -> builder.build(
                new RecoveryRequest(null, "hr", null, "respaldo", "hr", false)));
    }
}
