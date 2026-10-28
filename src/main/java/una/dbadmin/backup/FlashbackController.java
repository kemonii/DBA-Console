package una.dbadmin.backup;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import una.dbadmin.common.OperationResult;
import una.dbadmin.common.OracleIdentifiers;

@RestController
@RequestMapping("/api/flashback")
public class FlashbackController {

    private final FlashbackService service;

    public FlashbackController(FlashbackService service) {
        this.service = service;
    }

    @PostMapping("/table")
    public OperationResult restoreTable(@Valid @RequestBody FlashbackTableRequest request) {
        service.restoreTable(request);
        return OperationResult.ok("Table " + OracleIdentifiers.requireValid(request.schema()) + "."
                + OracleIdentifiers.requireValid(request.table()) + " restored from the recycle bin", null);
    }

    @PostMapping("/query")
    public OperationResult query(@Valid @RequestBody FlashbackQueryRequest request) {
        return OperationResult.ok("Rows as of " + request.timestamp(), service.queryAsOf(request));
    }

    @PostMapping("/restore")
    public OperationResult restoreRows(@Valid @RequestBody FlashbackQueryRequest request) {
        int restored = service.restoreRows(request);
        return OperationResult.ok(restored + " row(s) restored", restored);
    }
}
