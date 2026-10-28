package una.dbadmin.files;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

/**
 * HTTP endpoints for Oracle directory objects and the backup files stored in them.
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "File management")
public class FilesController {

    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    /** Request body to create a directory object. */
    public record CreateDirectoryRequest(String name, String path) {
    }

    /** Request body to grant READ and WRITE on a directory object. */
    public record GrantRequest(String user) {
    }

    @GetMapping("/directories")
    @Operation(summary = "List directory objects")
    public OperationResult listDirectories() {
        return OperationResult.ok("Directory objects listed", filesService.listDirectories());
    }

    @PostMapping("/directories")
    @Operation(summary = "Create or replace a directory object")
    public OperationResult createDirectory(@RequestBody CreateDirectoryRequest request) {
        String name = filesService.createDirectory(request.name(), request.path());
        return OperationResult.ok("Directory " + name + " created", name);
    }

    @DeleteMapping("/directories/{name}")
    @Operation(summary = "Drop a directory object")
    public OperationResult dropDirectory(@PathVariable String name) {
        filesService.dropDirectory(name);
        return OperationResult.ok("Directory dropped", null);
    }

    @PostMapping("/directories/{name}/grants")
    @Operation(summary = "Grant READ and WRITE on a directory object to a user")
    public OperationResult grantAccess(@PathVariable String name, @RequestBody GrantRequest request) {
        filesService.grantAccess(name, request.user());
        return OperationResult.ok("READ and WRITE granted", null);
    }

    @DeleteMapping("/directories/{name}/grants/{user}")
    @Operation(summary = "Revoke READ and WRITE on a directory object from a user")
    public OperationResult revokeAccess(@PathVariable String name, @PathVariable String user) {
        filesService.revokeAccess(name, user);
        return OperationResult.ok("READ and WRITE revoked", null);
    }

    @GetMapping("/directories/{name}/files")
    @Operation(summary = "List the .dmp and .log files of a directory object")
    public OperationResult listFiles(@PathVariable String name) {
        return OperationResult.ok("Files listed", filesService.listFiles(name));
    }

    @GetMapping("/directories/{name}/files/{file}/content")
    @Operation(summary = "View the content of a .log file")
    public OperationResult readLog(@PathVariable String name, @PathVariable String file) {
        return OperationResult.ok("Log read", filesService.readLog(name, file));
    }

    @DeleteMapping("/directories/{name}/files/{file}")
    @Operation(summary = "Delete a backup file", description = "Requires confirm=true.")
    public OperationResult deleteFile(
            @PathVariable String name,
            @PathVariable String file,
            @RequestParam(defaultValue = "false") boolean confirm) {
        filesService.deleteFile(name, file, confirm);
        return OperationResult.ok("File deleted", null);
    }
}
