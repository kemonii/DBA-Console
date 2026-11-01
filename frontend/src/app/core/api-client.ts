import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { ApiError } from './api-error';
import { OperationResult } from './operation-result.model';

const API_BASE = '/api';

/**
 * Single entry point for calling the backend.
 *
 * Paths are relative to `/api`, for example `get('/health')` calls `GET /api/health`.
 * Successful responses are returned as `OperationResult<T>`. Failures are raised as
 * `ApiError` carrying the backend message when there is one, so screens only need
 * to handle one error type.
 */
@Service()
export class ApiClient {
  private readonly http = inject(HttpClient);

  get<T = unknown>(path: string): Observable<OperationResult<T>> {
    return this.http.get<OperationResult<T>>(API_BASE + path).pipe(catchError(toApiError));
  }

  post<T = unknown>(path: string, body: unknown): Observable<OperationResult<T>> {
    return this.http.post<OperationResult<T>>(API_BASE + path, body).pipe(catchError(toApiError));
  }

  put<T = unknown>(path: string, body: unknown): Observable<OperationResult<T>> {
    return this.http.put<OperationResult<T>>(API_BASE + path, body).pipe(catchError(toApiError));
  }

  delete<T = unknown>(path: string): Observable<OperationResult<T>> {
    return this.http.delete<OperationResult<T>>(API_BASE + path).pipe(catchError(toApiError));
  }
}

function toApiError(error: unknown): Observable<never> {
  if (!(error instanceof HttpErrorResponse)) {
    return throwError(() => new ApiError('Error inesperado', 0));
  }
  if (error.status === 0) {
    return throwError(
      () => new ApiError('No se puede conectar con el servidor. Verifique que el backend esté en ejecución.', 0),
    );
  }
  if (isOperationResult(error.error)) {
    return throwError(() => new ApiError(error.error.message, error.status, error.error.data));
  }
  return throwError(() => new ApiError(`La solicitud falló con el estado ${error.status}`, error.status));
}

function isOperationResult(body: unknown): body is OperationResult {
  return (
    typeof body === 'object' && body !== null && typeof (body as OperationResult).message === 'string'
  );
}
