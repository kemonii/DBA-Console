package una.dbadmin.tuning.monitoring;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

@Tag(name = "Performance monitoring")
@RestController
@RequestMapping("/api/tuning/monitoring")
public class MonitoringController {
    private final MonitoringService service;
    public MonitoringController(MonitoringService service) {
        this.service = service;
    }
    @GetMapping("/instance")
    public OperationResult instance() {
        return OperationResult.ok("Instance status", service.instance());
    }

    @GetMapping("/parameters")
    public OperationResult parameters() {
        return OperationResult.ok("Instance parameters", service.parameters());
    }

    @GetMapping("/sessions")
    public OperationResult sessions() {
        return OperationResult.ok("Current sessions", service.sessions());
    }

    @GetMapping("/sessions/programs")
    public OperationResult programs() {
        return OperationResult.ok("Sessions per program", service.sessionsByProgram());
    }

    @GetMapping("/sessions/users")
    public OperationResult users() {
        return OperationResult.ok("Sessions per user", service.sessionsByUser());
    }

    @GetMapping("/locks")
    public OperationResult locks() {
        return OperationResult.ok("Blocking locks", service.blockers());
    }

    @GetMapping("/locks/objects")
    public OperationResult objects() {
        return OperationResult.ok("Locked objects", service.lockedObjects());
    }

    @DeleteMapping("/sessions/{sid}/{serial}")
    public OperationResult kill(@PathVariable int sid, @PathVariable int serial, @RequestParam(defaultValue = "false") boolean confirm) {
        service.killBlocker(sid, serial, confirm);
        return OperationResult.ok("Blocking session marked for termination", null);
    }
}
