package una.dbadmin.security.tablespaces;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    @Operation(summary = "List tablespaces with their size, used and free space")
    public OperationResult list() {
        return OperationResult.ok("Tablespaces listed", tablespaceService.listUsage());
    }

    @GetMapping("/{name}/datafiles")
    @Operation(summary = "List the datafiles of a tablespace")
    public OperationResult listDatafiles(@PathVariable String name) {
        return OperationResult.ok("Datafiles listed", tablespaceService.listDatafiles(name));
    }
}
