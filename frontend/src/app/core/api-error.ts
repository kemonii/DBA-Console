/**
 * Error raised by `ApiClient` when a request fails.
 * `status` is the HTTP status code, or 0 when the server could not be reached.
 * `data` is the `data` of the backend's error response, for example the output of a failed command;
 * `null` when there is none.
 */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly data: unknown = null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
