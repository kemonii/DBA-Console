package una.dbadmin.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions thrown by any controller into an {@link OperationResult} with {@code ok=false}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Matches an Oracle error code and the rest of its line, e.g. "ORA-00942: table or view does not exist". */
    private static final Pattern ORACLE_ERROR = Pattern.compile("ORA-\\d{5}[^\\r\\n]*");

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<OperationResult> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(OperationResult.error(ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<OperationResult> handleDataAccess(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OperationResult.error(describe(ex)));
    }

    /**
     * Returns the first Oracle error found in the cause chain, so the ORA-xxxxx code reaches the client.
     * Falls back to the first line of the most specific cause when no Oracle code is present.
     */
    private static String describe(DataAccessException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null) {
                Matcher m = ORACLE_ERROR.matcher(t.getMessage());
                if (m.find()) {
                    return m.group().trim();
                }
            }
        }
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail == null || detail.isBlank()) {
            return "Database error";
        }
        return "Database error: " + detail.lines().findFirst().orElse(detail).trim();
    }
}
