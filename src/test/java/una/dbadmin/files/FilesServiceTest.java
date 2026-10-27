package una.dbadmin.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class FilesServiceTest {

    private static final String PATH_QUERY = "SELECT directory_path FROM dba_directories WHERE directory_name = ?";

    @TempDir
    Path backupDir;

    private JdbcTemplate jdbc;
    private FilesService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new FilesService(jdbc);
    }

    // ---- directory objects -------------------------------------------------------------------------------------

    @Test
    void listDirectoriesReadsTheDataDictionary() {
        when(jdbc.queryForList("SELECT directory_name, directory_path FROM dba_directories ORDER BY directory_name"))
                .thenReturn(List.of(Map.of("DIRECTORY_NAME", "BACKUPS", "DIRECTORY_PATH", "/u01/backups")));

        List<DirectoryObject> result = service.listDirectories();

        assertEquals(List.of(new DirectoryObject("BACKUPS", "/u01/backups")), result);
    }

    @Test
    void createDirectoryUsesUppercaseIdentifierAndQuotedPath() {
        service.createDirectory("backups", "C:\\app\\oracle\\backups");

        verify(jdbc).execute("CREATE OR REPLACE DIRECTORY BACKUPS AS 'C:\\app\\oracle\\backups'");
    }

    @Test
    void createDirectoryReturnsTheNormalizedName() {
        assertEquals("BACKUPS", service.createDirectory("backups", "/u01/backups"));
    }

    @Test
    void createDirectoryRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("bad name", "/u01"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("x; DROP USER a", "/u01"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory(null, "/u01"));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void createDirectoryRejectsUnsafePaths() {
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", null));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "  "));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "/u01/it's"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "/u01'; DROP USER a; --"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "/u01\u0000/x"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "/u01\n/x"));
        assertThrows(IllegalArgumentException.class, () -> service.createDirectory("D", "x".repeat(4001)));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void grantAccessGrantsReadAndWriteToTheUser() {
        service.grantAccess("backups", "scott");

        verify(jdbc).execute("GRANT READ, WRITE ON DIRECTORY BACKUPS TO SCOTT");
    }

    @Test
    void revokeAccessRevokesReadAndWriteFromTheUser() {
        service.revokeAccess("backups", "scott");

        verify(jdbc).execute("REVOKE READ, WRITE ON DIRECTORY BACKUPS FROM SCOTT");
    }

    @Test
    void grantAndRevokeRejectInvalidIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> service.grantAccess("backups", "scott; DROP USER x"));
        assertThrows(IllegalArgumentException.class, () -> service.grantAccess("bad name", "scott"));
        assertThrows(IllegalArgumentException.class, () -> service.revokeAccess("backups", ""));
        assertThrows(IllegalArgumentException.class, () -> service.revokeAccess(null, "scott"));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void dropDirectoryUsesValidatedIdentifier() {
        service.dropDirectory("backups");

        verify(jdbc).execute("DROP DIRECTORY BACKUPS");
    }

    @Test
    void dropDirectoryRejectsInvalidIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> service.dropDirectory("a b"));
        verify(jdbc, never()).execute(anyString());
    }

    // ---- backup files ------------------------------------------------------------------------------------------

    @Test
    void listFilesReturnsOnlyDumpAndLogFilesSortedByName() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.write(backupDir.resolve("b.dmp"), new byte[2048]);
        Files.writeString(backupDir.resolve("a.log"), "hello");
        Files.writeString(backupDir.resolve("notes.txt"), "ignored");
        Files.writeString(backupDir.resolve("C.DMP"), "x");
        Files.createDirectory(backupDir.resolve("folder.dmp"));

        List<BackupFile> files = service.listFiles("backups");

        assertEquals(List.of("C.DMP", "a.log", "b.dmp"), files.stream().map(BackupFile::name).toList());
        BackupFile dump = files.get(2);
        assertEquals(2048L, dump.sizeBytes());
        assertEquals(Files.getLastModifiedTime(backupDir.resolve("b.dmp")).toInstant(), dump.lastModified());
    }

    @Test
    void listFilesUsesABindParameterForTheDirectoryName() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);

        service.listFiles("backups");

        verify(jdbc).queryForList(PATH_QUERY, String.class, "BACKUPS");
    }

    @Test
    void listFilesFailsWhenTheDirectoryObjectDoesNotExist() {
        when(jdbc.queryForList(PATH_QUERY, String.class, "MISSING")).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.listFiles("missing"));

        assertTrue(ex.getMessage().contains("MISSING"));
    }

    @Test
    void listFilesFailsWhenThePathDoesNotExistOnThisHost() {
        directoryIsMappedTo("GHOST", backupDir.resolve("does-not-exist"));

        assertThrows(IllegalArgumentException.class, () -> service.listFiles("ghost"));
    }

    @Test
    void readLogReturnsTheFileContent() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("export.log"), "line 1\nline 2\n", StandardCharsets.UTF_8);

        LogContent log = service.readLog("backups", "export.log");

        assertEquals("export.log", log.name());
        assertEquals("line 1\nline 2\n", log.content());
        assertFalse(log.truncated());
    }

    @Test
    void readLogTruncatesAfterTheMaximumNumberOfLines() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        String text = String.join("\n", IntStream.rangeClosed(1, FilesService.MAX_LOG_LINES + 500)
                .mapToObj(i -> "line " + i).toList());
        Files.writeString(backupDir.resolve("big.log"), text);

        LogContent log = service.readLog("backups", "big.log");

        assertTrue(log.truncated());
        assertEquals(FilesService.MAX_LOG_LINES, log.content().lines().count());
    }

    @Test
    void readLogTruncatesAfterTheMaximumNumberOfCharacters() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("wide.log"), "x".repeat(FilesService.MAX_LOG_CHARS + 10));

        LogContent log = service.readLog("backups", "wide.log");

        assertTrue(log.truncated());
        assertEquals(FilesService.MAX_LOG_CHARS, log.content().length());
    }

    @Test
    void readLogToleratesNonUtf8Bytes() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.write(backupDir.resolve("latin.log"), new byte[] {'o', 'k', (byte) 0xE9, '\n'});

        LogContent log = service.readLog("backups", "latin.log");

        assertTrue(log.content().startsWith("ok"));
    }

    @Test
    void readLogRejectsDumpFiles() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Files.writeString(backupDir.resolve("data.dmp"), "binary");

        assertThrows(IllegalArgumentException.class, () -> service.readLog("backups", "data.dmp"));
    }

    @Test
    void readLogFailsWhenTheFileDoesNotExist() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);

        assertThrows(IllegalArgumentException.class, () -> service.readLog("backups", "missing.log"));
    }

    @Test
    void deleteFileRemovesTheFileWhenConfirmed() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path dump = Files.writeString(backupDir.resolve("old.dmp"), "x");

        service.deleteFile("backups", "old.dmp", true);

        assertFalse(Files.exists(dump));
    }

    @Test
    void deleteFileRequiresExplicitConfirmation() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path dump = Files.writeString(backupDir.resolve("old.dmp"), "x");

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "old.dmp", false));

        assertTrue(Files.exists(dump));
    }

    @Test
    void deleteFileFailsWhenTheFileDoesNotExist() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "ghost.dmp", true));
    }

    @Test
    void deleteFileDoesNotDeleteFilesOfOtherTypes() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path notes = Files.writeString(backupDir.resolve("notes.txt"), "keep");

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "notes.txt", true));

        assertTrue(Files.exists(notes));
    }

    @Test
    void deleteFileDoesNotDeleteDirectories() throws IOException {
        directoryIsMappedTo("BACKUPS", backupDir);
        Path folder = Files.createDirectory(backupDir.resolve("folder.dmp"));

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "folder.dmp", true));

        assertTrue(Files.exists(folder));
    }

    @Test
    void fileOperationsRejectTraversalAttempts() throws IOException {
        Path root = Files.createDirectory(backupDir.resolve("root"));
        Path outside = Files.writeString(backupDir.resolve("outside.dmp"), "secret");
        directoryIsMappedTo("BACKUPS", root);

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "../outside.dmp", true));
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "..\\outside.dmp", true));
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteFile("backups", outside.toAbsolutePath().toString(), true));
        assertThrows(IllegalArgumentException.class, () -> service.readLog("backups", "../outside.log"));

        assertTrue(Files.exists(outside));
    }

    @Test
    void fileOperationsRejectSymlinksThatEscapeTheDirectory() throws IOException {
        Path root = Files.createDirectory(backupDir.resolve("root"));
        Path outside = Files.writeString(backupDir.resolve("outside.dmp"), "secret");
        Path link = root.resolve("link.dmp");
        assumeTrue(trySymlink(link, outside), "symbolic links are not available on this machine");
        directoryIsMappedTo("BACKUPS", root);

        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("backups", "link.dmp", true));

        assertTrue(Files.exists(outside));
        assertTrue(Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    void listFilesSkipsSymlinksThatEscapeTheDirectory() throws IOException {
        Path root = Files.createDirectory(backupDir.resolve("root"));
        Path outside = Files.writeString(backupDir.resolve("outside.dmp"), "secret");
        Files.writeString(root.resolve("inside.dmp"), "ok");
        assumeTrue(trySymlink(root.resolve("link.dmp"), outside), "symbolic links are not available on this machine");
        directoryIsMappedTo("BACKUPS", root);

        List<BackupFile> files = service.listFiles("backups");

        assertEquals(List.of("inside.dmp"), files.stream().map(BackupFile::name).toList());
    }

    @Test
    void backupFileKeepsAnInstantForTheModificationTime() {
        Instant now = Instant.now();

        assertEquals(now, new BackupFile("a.dmp", 1L, now).lastModified());
    }

    private void directoryIsMappedTo(String name, Path path) {
        when(jdbc.queryForList(PATH_QUERY, String.class, name)).thenReturn(List.of(path.toString()));
    }

    private static boolean trySymlink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            return false;
        }
    }
}
