package una.dbadmin.security.auditing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

/**
 * HTTP endpoints for auditing.
 */
@RestController
@RequestMapping("/api/security/auditing")
@Tag(name = "Auditing")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/parameter")
    @Operation(summary = "Show the audit_trail parameter")
    public OperationResult auditTrail() {
        return OperationResult.ok("Parameter read", auditService.auditTrail());
    }

    @GetMapping("/options")
    @Operation(summary = "List the statement and object audit options in effect")
    public OperationResult options() {
        return OperationResult.ok("Audit options listed", auditService.options());
    }
}
