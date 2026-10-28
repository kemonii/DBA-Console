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
 * HTTP endpoints for database users.
 */
@RestController
@RequestMapping("/api/security/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Request body to create a user. Tablespaces and quota are optional.
     *
     * @param quotaMb        quota in MB on the default tablespace
     * @param unlimitedQuota {@code QUOTA UNLIMITED} on the default tablespace
     */
    public record CreateUserRequest(String username, String password, String defaultTablespace,
                                    String temporaryTablespace, Integer quotaMb, boolean unlimitedQuota) {
    }

    @GetMapping
    @Operation(summary = "List the users created by administrators (not maintained by Oracle)")
    public OperationResult list() {
        return OperationResult.ok("Users listed", userService.list());
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public OperationResult create(@RequestBody CreateUserRequest request) {
        String name = userService.create(request.username(), request.password(), request.defaultTablespace(),
                request.temporaryTablespace(), request.quotaMb(), request.unlimitedQuota());
        return OperationResult.ok("User " + name + " created", name);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Drop a user", description = "Requires confirm=true; cascade=true also drops its objects.")
    public OperationResult drop(@PathVariable String name,
                                @RequestParam(defaultValue = "false") boolean cascade,
                                @RequestParam(defaultValue = "false") boolean confirm) {
        userService.drop(name, cascade, confirm);
        return OperationResult.ok("User dropped", null);
    }
}
