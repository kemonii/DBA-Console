package una.dbadmin.security.auditing;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** Request body to audit sessions; {@code whenever} defaults to {@code ALWAYS}. */
    public record SessionAuditRequest(Whenever whenever) {
    }

    /**
     * Request body to audit actions on a table.
     *
     * @param actions  any of INSERT, UPDATE, DELETE, SELECT
     * @param byAccess {@code BY ACCESS} (one record per statement) instead of {@code BY SESSION}
     */
    public record TableAuditRequest(String owner, String table, List<String> actions, boolean byAccess) {
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

    @PostMapping("/session")
    @Operation(summary = "Audit logins (AUDIT SESSION)")
    public OperationResult auditSessions(@RequestBody SessionAuditRequest request) {
        auditService.auditSessions(request.whenever());
        return OperationResult.ok("Session auditing enabled", null);
    }

    @DeleteMapping("/session")
    @Operation(summary = "Stop auditing logins (NOAUDIT SESSION)")
    public OperationResult stopAuditingSessions(@RequestParam(defaultValue = "ALWAYS") Whenever whenever) {
        auditService.stopAuditingSessions(whenever);
        return OperationResult.ok("Session auditing disabled", null);
    }

    @PostMapping("/tables")
    @Operation(summary = "Audit actions on a table (AUDIT ... ON owner.table BY ACCESS)")
    public OperationResult auditTable(@RequestBody TableAuditRequest request) {
        auditService.auditTable(request.owner(), request.table(), request.actions(), request.byAccess());
        return OperationResult.ok("Table auditing enabled", null);
    }

    @DeleteMapping("/tables")
    @Operation(summary = "Stop auditing actions on a table (NOAUDIT)", description = "actions is comma-separated.")
    public OperationResult stopAuditingTable(@RequestParam String owner, @RequestParam String table,
                                             @RequestParam List<String> actions) {
        auditService.stopAuditingTable(owner, table, actions);
        return OperationResult.ok("Table auditing disabled", null);
    }

    @GetMapping("/trail")
    @Operation(summary = "Newest rows of DBA_AUDIT_TRAIL", description = "Optional username and limit (default 100).")
    public OperationResult trail(@RequestParam(required = false) String username,
                                 @RequestParam(required = false) Integer limit) {
        return OperationResult.ok("Audit trail read", auditService.trail(username, limit));
    }

    @GetMapping("/trail/sessions")
    @Operation(summary = "Newest login attempts from DBA_AUDIT_SESSION")
    public OperationResult sessions(@RequestParam(required = false) String username,
                                    @RequestParam(required = false) Integer limit) {
        return OperationResult.ok("Session audit read", auditService.sessions(username, limit));
    }

    @GetMapping("/trail/objects")
    @Operation(summary = "Newest audited actions on objects from DBA_AUDIT_OBJECT")
    public OperationResult objects(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) Integer limit) {
        return OperationResult.ok("Object audit read", auditService.objects(username, limit));
    }
}
