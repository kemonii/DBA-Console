import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import {
  BackupRequest,
  DataPumpResult,
  FlashbackQueryRequest,
  FlashbackTableRequest,
  OperationResult,
  RecoveryRequest,
  Row,
} from './backup.models';

/**
 * Client of the backup, recovery and flashback endpoints. Failures never reach the caller as errors: the API answers
 * with an {@link OperationResult} even when it rejects a request, so every call completes with one.
 */
@Injectable({ providedIn: 'root' })
export class BackupApiService {
  private readonly http = inject(HttpClient);

  backup(request: BackupRequest): Observable<OperationResult<DataPumpResult>> {
    return this.post('/api/backup', request);
  }

  recover(request: RecoveryRequest): Observable<OperationResult<DataPumpResult>> {
    return this.post('/api/recovery', request);
  }

  flashbackTable(request: FlashbackTableRequest): Observable<OperationResult<null>> {
    return this.post('/api/flashback/table', request);
  }

  flashbackQuery(request: FlashbackQueryRequest): Observable<OperationResult<Row[]>> {
    return this.post('/api/flashback/query', request);
  }

  flashbackRestore(request: FlashbackQueryRequest): Observable<OperationResult<number>> {
    return this.post('/api/flashback/restore', request);
  }

  readLog(directory: string, fileName: string): Observable<OperationResult<string[]>> {
    const params = new HttpParams().set('directory', directory).set('fileName', fileName);
    return this.http
      .get<OperationResult<string[]>>('/api/backup/log', { params })
      .pipe(catchError((error) => of(toResult<string[]>(error))));
  }

  private post<T>(url: string, body: unknown): Observable<OperationResult<T>> {
    return this.http.post<OperationResult<T>>(url, body).pipe(catchError((error) => of(toResult<T>(error))));
  }
}

function toResult<T>(error: unknown): OperationResult<T> {
  if (error instanceof HttpErrorResponse) {
    if (isOperationResult<T>(error.error)) {
      return error.error;
    }
    if (error.status !== 0) {
      return { ok: false, message: `Request failed (HTTP ${error.status})`, data: null };
    }
  }
  return { ok: false, message: 'Could not reach the server', data: null };
}

function isOperationResult<T>(body: unknown): body is OperationResult<T> {
  return typeof body === 'object' && body !== null && 'ok' in body && 'message' in body;
}
