package una.dbadmin.security.users;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

/**
 * HTTP endpoints for profiles.
 */
@RestController
@RequestMapping("/api/security/profiles")
@Tag(name = "Profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Request body to create a profile.
     *
     * @param limits resource name to value, for example {@code {"SESSIONS_PER_USER": "2", "IDLE_TIME": "30"}}
     */
    public record CreateProfileRequest(String name, Map<String, String> limits) {
    }

    /** Request body to change limits of a profile; only the given resources change. */
    public record AlterProfileRequest(Map<String, String> limits) {
    }

    @GetMapping
    @Operation(summary = "List profiles and their limits (DBA_PROFILES)")
    public OperationResult list() {
        return OperationResult.ok("Profiles listed", profileService.list());
    }

    @PostMapping
    @Operation(summary = "Create a profile")
    public OperationResult create(@RequestBody CreateProfileRequest request) {
        String name = profileService.create(request.name(), request.limits());
        return OperationResult.ok("Profile " + name + " created", name);
    }

    @PutMapping("/{name}")
    @Operation(summary = "Change limits of a profile (ALTER PROFILE)")
    public OperationResult alter(@PathVariable String name, @RequestBody AlterProfileRequest request) {
        profileService.alter(name, request.limits());
        return OperationResult.ok("Profile changed", null);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Drop a profile; its users get the DEFAULT profile")
    public OperationResult drop(@PathVariable String name) {
        profileService.drop(name);
        return OperationResult.ok("Profile dropped", null);
    }
}
