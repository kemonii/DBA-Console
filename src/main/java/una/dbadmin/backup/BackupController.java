package una.dbadmin.backup;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import una.dbadmin.common.OperationResult;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OperationResult> backup(@Valid @RequestBody BackupRequest request) {
        BackupResult result = service.backup(request);
        if (result.success()) {
            return ResponseEntity.ok(OperationResult.ok("Backup completed", result));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OperationResult(false, "Backup failed with exit code " + result.exitCode(), result));
    }
}
