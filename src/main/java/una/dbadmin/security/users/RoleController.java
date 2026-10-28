package una.dbadmin.security.users;

import org.springframework.web.bind.annotation.DeleteMapping;
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
 * HTTP endpoints for roles.
 */
@RestController
@RequestMapping("/api/security/roles")
@Tag(name = "Roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** Request body to create a role. */
    public record CreateRoleRequest(String name) {
    }

    @GetMapping
    @Operation(summary = "List roles, the ones created by administrators first")
    public OperationResult list() {
        return OperationResult.ok("Roles listed", roleService.list());
    }

    @PostMapping
    @Operation(summary = "Create a role")
    public OperationResult create(@RequestBody CreateRoleRequest request) {
        String name = roleService.create(request.name());
        return OperationResult.ok("Role " + name + " created", name);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Drop a role")
    public OperationResult drop(@PathVariable String name) {
        roleService.drop(name);
        return OperationResult.ok("Role dropped", null);
    }
}
