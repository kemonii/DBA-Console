package una.dbadmin.common;

import java.io.IOException;
import java.io.UncheckedIOException;
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
     * File system and process failures, such as an unreadable log or {@code expdp} missing from the PATH. A message
     * written for the user is returned as it is; an exception that only wraps its cause reports the cause's message.
     */
    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<OperationResult> handleUncheckedIo(UncheckedIOException ex) {
        log.error("I/O error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OperationResult.error(describe(ex)));
    }

    /**
     * Failures that are not caused by the request, such as a Data Pump run that timed out or missing configuration.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<OperationResult> handleIllegalState(IllegalStateException ex) {
        log.error("Operation failed", ex);
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "La operación falló" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(OperationResult.error(message));
    }

    private static String describe(UncheckedIOException ex) {
        IOException cause = ex.getCause();
        boolean onlyWrapsCause = cause != null && String.valueOf(cause).equals(ex.getMessage());
        if (!onlyWrapsCause && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        String detail = cause != null && cause.getMessage() != null ? cause.getMessage() : "error desconocido";
        return "Error del sistema de archivos: " + detail;
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
            return "Error de base de datos";
        }
        return "Error de base de datos: " + detail.lines().findFirst().orElse(detail).trim();
    }
}
