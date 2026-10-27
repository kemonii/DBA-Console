/**
 * Error raised by `ApiClient` when a request fails.
 * `status` is the HTTP status code, or 0 when the server could not be reached.
 */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
