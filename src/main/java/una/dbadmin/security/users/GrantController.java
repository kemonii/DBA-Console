package una.dbadmin.security.users;

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
 * HTTP endpoints to grant and revoke privileges and roles. Revokes take their values as query parameters, since a
 * DELETE request has no body.
 */
@RestController
@RequestMapping("/api/security/grants")
@Tag(name = "Privileges")
public class GrantController {

    private final GrantService grantService;

    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    /** Request body to grant a system privilege. */
    public record SystemPrivilegeGrantRequest(String privilege, String grantee, boolean adminOption) {
    }

    /** Request body to grant a role. */
    public record RoleGrantRequest(String role, String grantee, boolean adminOption) {
    }

    @GetMapping("/system-privileges")
    @Operation(summary = "List the system privileges that can be granted (SYSTEM_PRIVILEGE_MAP)")
    public OperationResult listSystemPrivilegeNames() {
        return OperationResult.ok("System privileges listed", grantService.listSystemPrivilegeNames());
    }

    @GetMapping("/{grantee}")
    @Operation(summary = "Show the privileges and roles of a user or role")
    public OperationResult privilegesOf(@PathVariable String grantee) {
        return OperationResult.ok("Privileges listed", grantService.privilegesOf(grantee));
    }

    @PostMapping("/system")
    @Operation(summary = "Grant a system privilege")
    public OperationResult grantSystemPrivilege(@RequestBody SystemPrivilegeGrantRequest request) {
        grantService.grantSystemPrivilege(request.privilege(), request.grantee(), request.adminOption());
        return OperationResult.ok("Privilege granted", null);
    }

    @DeleteMapping("/system")
    @Operation(summary = "Revoke a system privilege")
    public OperationResult revokeSystemPrivilege(@RequestParam String privilege, @RequestParam String grantee) {
        grantService.revokeSystemPrivilege(privilege, grantee);
        return OperationResult.ok("Privilege revoked", null);
    }

    @PostMapping("/role")
    @Operation(summary = "Grant a role")
    public OperationResult grantRole(@RequestBody RoleGrantRequest request) {
        grantService.grantRole(request.role(), request.grantee(), request.adminOption());
        return OperationResult.ok("Role granted", null);
    }

    @DeleteMapping("/role")
    @Operation(summary = "Revoke a role")
    public OperationResult revokeRole(@RequestParam String role, @RequestParam String grantee) {
        grantService.revokeRole(role, grantee);
        return OperationResult.ok("Role revoked", null);
    }
}
