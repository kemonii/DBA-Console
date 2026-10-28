package una.dbadmin.security.users;

import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    @Operation(summary = "List profiles and their limits (DBA_PROFILES)")
    public OperationResult list() {
        return OperationResult.ok("Profiles listed", profileService.list());
    }
}
