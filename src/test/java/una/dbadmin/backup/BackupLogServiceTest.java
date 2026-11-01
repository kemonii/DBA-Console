package una.dbadmin.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class BackupLogServiceTest {

    @TempDir
    Path directory;

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BackupLogService service = new BackupLogService(jdbc);

    @BeforeEach
    void directoryObjectPointsToTheTempDir() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("RESPALDO")))
                .thenReturn(List.of(directory.toString()));
    }

    @Test
    void readsTheLogFromTheDirectoryObjectPath() throws IOException {
        Files.writeString(directory.resolve("emp.log"), "Export started\nJob completed\n");

        assertEquals(List.of("Export started", "Job completed"), service.read("respaldo", "emp.log"));
    }

    @Test
    void acceptsTheExtensionInUpperCase() throws IOException {
        Files.writeString(directory.resolve("NW.LOG"), "Import started\n");

        assertEquals(List.of("Import started"), service.read("respaldo", "NW.LOG"));
    }

    @Test
    void returnsOnlyTheLastLinesOfALongLog() throws IOException {
        Files.write(directory.resolve("big.log"), IntStream.rangeClosed(1, 600).mapToObj(i -> "line " + i).toList());

        List<String> lines = service.read("respaldo", "big.log");

        assertEquals(500, lines.size());
        assertEquals("line 101", lines.get(0));
        assertEquals("line 600", lines.get(499));
    }

    @Test
    void reportsAMissingLogFile() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> service.read("respaldo", "missing.log"));

        assertEquals("No se encontró el archivo de log missing.log en el directorio RESPALDO", error.getMessage());
    }

    @Test
    void reportsAnUnknownDirectoryObject() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("NOPE"))).thenReturn(List.of());

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> service.read("nope", "emp.log"));

        assertEquals("El directorio NOPE no existe", error.getMessage());
    }

    @Test
    void onlyLogFilesWithPlainNamesAreAccepted() {
        for (String name : List.of("../emp.log", "emp.dmp", "emp", "sub/emp.log", "emp.log.txt", "")) {
            assertThrows(IllegalArgumentException.class, () -> service.read("respaldo", name), name);
        }
        assertThrows(IllegalArgumentException.class, () -> service.read("resp aldo", "emp.log"));
        assertThrows(IllegalArgumentException.class, () -> service.read("respaldo", null));
    }

    @Test
    void invalidNamesNeverReachTheDatabase() {
        JdbcTemplate untouched = mock(JdbcTemplate.class);

        assertThrows(IllegalArgumentException.class, () -> new BackupLogService(untouched).read("respaldo", "../x.log"));
        assertThrows(IllegalArgumentException.class, () -> new BackupLogService(untouched).read("x; drop", "x.log"));

        verifyNoInteractions(untouched);
    }
}
