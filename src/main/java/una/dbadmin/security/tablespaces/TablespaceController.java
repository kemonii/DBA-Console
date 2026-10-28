package una.dbadmin.security.tablespaces;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

/**
 * HTTP endpoints for tablespaces and their datafiles.
 */
@RestController
@RequestMapping("/api/security/tablespaces")
@Tag(name = "Tablespaces")
public class TablespaceController {

    private final TablespaceService tablespaceService;

    public TablespaceController(TablespaceService tablespaceService) {
        this.tablespaceService = tablespaceService;
    }

    /**
     * Request body to create a tablespace with one autoextensible datafile.
     *
     * @param maxSizeMb omit for {@code MAXSIZE UNLIMITED}
     */
    public record CreateTablespaceRequest(String name, String fileName, Integer sizeMb, Integer nextMb,
                                          Integer maxSizeMb) {
    }

    /** Request body to add a datafile to a tablespace. */
    public record AddDatafileRequest(String fileName, Integer sizeMb) {
    }

    @GetMapping
    @Operation(summary = "List tablespaces with their size, used and free space")
    public OperationResult list() {
        return OperationResult.ok("Tablespaces listed", tablespaceService.listUsage());
    }

    @PostMapping
    @Operation(summary = "Create a tablespace with an autoextensible datafile in DATAFILE_DIR")
    public OperationResult create(@RequestBody CreateTablespaceRequest request) {
        String name = tablespaceService.create(
                request.name(), request.fileName(), request.sizeMb(), request.nextMb(), request.maxSizeMb());
        return OperationResult.ok("Tablespace " + name + " created", name);
    }

    @GetMapping("/{name}/datafiles")
    @Operation(summary = "List the datafiles of a tablespace")
    public OperationResult listDatafiles(@PathVariable String name) {
        return OperationResult.ok("Datafiles listed", tablespaceService.listDatafiles(name));
    }

    @PostMapping("/{name}/datafiles")
    @Operation(summary = "Add a datafile in DATAFILE_DIR to a tablespace")
    public OperationResult addDatafile(@PathVariable String name, @RequestBody AddDatafileRequest request) {
        tablespaceService.addDatafile(name, request.fileName(), request.sizeMb());
        return OperationResult.ok("Datafile added", null);
    }
}
