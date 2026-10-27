import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import {
  BackupRequest,
  DataPumpResult,
  FlashbackQueryRequest,
  FlashbackTableRequest,
  RecoveryRequest,
  Row,
} from './backup.models';

/**
 * Calls of the backup module. Failures are raised as `ApiError` by `ApiClient`; for a failed Data Pump run the
 * error's `data` holds the command and the output.
 */
@Service()
export class BackupApi {
  private readonly api = inject(ApiClient);

  backup(request: BackupRequest): Observable<OperationResult<DataPumpResult>> {
    return this.api.post('/backup', request);
  }

  recover(request: RecoveryRequest): Observable<OperationResult<DataPumpResult>> {
    return this.api.post('/recovery', request);
  }

  readLog(directory: string, fileName: string): Observable<OperationResult<string[]>> {
    const query = `directory=${encodeURIComponent(directory)}&fileName=${encodeURIComponent(fileName)}`;
    return this.api.get(`/backup/log?${query}`);
  }

  flashbackTable(request: FlashbackTableRequest): Observable<OperationResult<null>> {
    return this.api.post('/flashback/table', request);
  }

  flashbackQuery(request: FlashbackQueryRequest): Observable<OperationResult<Row[]>> {
    return this.api.post('/flashback/query', request);
  }

  flashbackRestore(request: FlashbackQueryRequest): Observable<OperationResult<number>> {
    return this.api.post('/flashback/restore', request);
  }
}
