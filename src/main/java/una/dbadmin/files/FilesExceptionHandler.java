package una.dbadmin.files;

import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import una.dbadmin.common.OperationResult;

/**
 * Translates file system failures of the files module into an {@link OperationResult}. Database and invalid-input
 * errors are handled by the global handler.
 */
@RestControllerAdvice(assignableTypes = FilesController.class)
public class FilesExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FilesExceptionHandler.class);

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<OperationResult> handleIo(UncheckedIOException ex) {
        log.error("File system error", ex);
        String detail = ex.getCause() != null && ex.getCause().getMessage() != null
                ? ex.getCause().getMessage()
                : "unknown error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OperationResult.error("File system error: " + detail));
    }
}
