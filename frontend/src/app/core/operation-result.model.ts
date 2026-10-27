/**
 * Uniform response body returned by every backend endpoint.
 * Mirrors `una.dbadmin.common.OperationResult`.
 */
export interface OperationResult<T = unknown> {
  /** Whether the operation succeeded. */
  ok: boolean;
  /** Human-readable description of the outcome. */
  message: string;
  /** Optional payload; `null` when there is nothing to return. */
  data: T | null;
}
