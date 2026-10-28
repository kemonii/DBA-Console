package una.dbadmin.backup;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import una.dbadmin.common.OperationResult;

@RestController
@RequestMapping("/api/backup/log")
public class BackupLogController {

    private final BackupLogService service;

    public BackupLogController(BackupLogService service) {
        this.service = service;
    }

    @GetMapping
    public OperationResult read(@RequestParam String directory, @RequestParam String fileName) {
        return OperationResult.ok("Contents of " + fileName, service.read(directory, fileName));
    }
}
