package una.dbadmin.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports whether the application can reach the configured Oracle database.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public OperationResult health() {
        // Connection or query failures propagate as DataAccessException to GlobalExceptionHandler.
        jdbcTemplate.queryForObject("SELECT 1 FROM dual", Integer.class);
        return OperationResult.ok("Database connection is available", null);
    }
}
