package una.dbadmin.common;

/**
 * Uniform response body returned by every endpoint.
 *
 * @param ok      whether the operation succeeded
 * @param message human-readable description of the outcome
 * @param data    optional payload; {@code null} when there is nothing to return
 */
public record OperationResult(boolean ok, String message, Object data) {

    public static OperationResult ok(String message, Object data) {
        return new OperationResult(true, message, data);
    }

    public static OperationResult error(String message) {
        return new OperationResult(false, message, null);
    }
}
