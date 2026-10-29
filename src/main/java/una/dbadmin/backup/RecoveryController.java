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
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryService service;

    public RecoveryController(RecoveryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OperationResult> recover(@Valid @RequestBody RecoveryRequest request) {
        RecoveryResult result = service.recover(request);
        if (result.success()) {
            return ResponseEntity.ok(OperationResult.ok("Recuperación completada", result));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OperationResult(false, "La recuperación falló con el código de salida " + result.exitCode(), result));
    }
}
