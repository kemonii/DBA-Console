package una.dbadmin.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BackupFileNamesTest {

    @ParameterizedTest
    @ValueSource(strings = {"expdp_full.dmp", "EXPDP_FULL.DMP", "export-01.log", "hr_schema_2024.01.dmp", "a.log"})
    void acceptsPlainBackupFileNames(String name) {
        assertEquals(name, BackupFileNames.requireValid(name));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   .dmp"})
    void rejectsNullAndBlank(String name) {
        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "..", "../secret.dmp", "..\\secret.dmp", "a/../b.dmp", "a..b.dmp", "..dmp.log",
            "sub/file.dmp", "sub\\file.dmp", "/etc/passwd.log", "\\windows\\system.log",
            "C:\\temp\\file.dmp", "C:file.dmp", "file.dmp:stream", "~/file.dmp"})
    void rejectsTraversalAndSeparators(String name) {
        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file.txt", "file", "file.dmp.exe", "file.dmpx", "dmp", ".dmp", ".log", "file.sql"})
    void rejectsOtherExtensions(String name) {
        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file name.dmp", "file\u0000.dmp", "file\n.log", "fi'le.dmp", "file;rm.dmp", "fi$le.dmp"})
    void rejectsUnsafeCharacters(String name) {
        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CON.dmp", "nul.log", "Aux.log", "COM1.dmp", "lpt9.log"})
    void rejectsWindowsReservedDeviceNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(name));
    }

    @Test
    void rejectsNamesLongerThan255Characters() {
        String tooLong = "a".repeat(252) + ".log";

        assertThrows(IllegalArgumentException.class, () -> BackupFileNames.requireValid(tooLong));
    }

    @Test
    void isLogRecognizesOnlyLogFiles() {
        assertEquals(true, BackupFileNames.isLog("export.log"));
        assertEquals(true, BackupFileNames.isLog("EXPORT.LOG"));
        assertEquals(false, BackupFileNames.isLog("export.dmp"));
    }
}
