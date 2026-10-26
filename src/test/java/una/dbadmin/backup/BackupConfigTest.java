package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BackupConfigTest {

    @Test
    void extractsEasyConnectIdentifierFromJdbcUrl() {
        assertEquals("//localhost:1521/XEPDB1",
                BackupConfig.connectIdentifier("jdbc:oracle:thin:@//localhost:1521/XEPDB1"));
    }

    @Test
    void rejectsUrlsThatAreNotOracleThin() {
        assertThrows(IllegalArgumentException.class, () -> BackupConfig.connectIdentifier("jdbc:mysql://localhost/db"));
    }
}
