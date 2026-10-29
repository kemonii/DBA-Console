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
        return OperationResult.ok("Estado de la instancia", service.instance());
    }

    @GetMapping("/parameters")
    public OperationResult parameters() {
        return OperationResult.ok("Parámetros de la instancia", service.parameters());
    }

    @GetMapping("/sessions")
    public OperationResult sessions() {
        return OperationResult.ok("Sesiones actuales", service.sessions());
    }

    @GetMapping("/sessions/programs")
    public OperationResult programs() {
        return OperationResult.ok("Sesiones por programa", service.sessionsByProgram());
    }

    @GetMapping("/sessions/users")
    public OperationResult users() {
        return OperationResult.ok("Sesiones por usuario", service.sessionsByUser());
    }

    @GetMapping("/locks")
    public OperationResult locks() {
        return OperationResult.ok("Bloqueos", service.blockers());
    }

    @GetMapping("/locks/objects")
    public OperationResult objects() {
        return OperationResult.ok("Objetos bloqueados", service.lockedObjects());
    }

    @DeleteMapping("/sessions/{sid}/{serial}")
    public OperationResult kill(@PathVariable int sid, @PathVariable int serial, @RequestParam(defaultValue = "false") boolean confirm) {
        service.killBlocker(sid, serial, confirm);
        return OperationResult.ok("Sesión bloqueadora marcada para terminar", null);
    }
}
