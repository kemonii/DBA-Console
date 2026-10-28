import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import { CreateTablespaceRequest, Datafile, TablespaceUsage } from './tablespaces.models';

const enc = encodeURIComponent;

/**
 * Calls the tablespace endpoints (`/api/security/tablespaces/...`).
 * Every path segment that comes from the user is URL-encoded here, so screens pass raw values.
 */
@Service()
export class TablespacesApi {
  private readonly api = inject(ApiClient);

  list(): Observable<OperationResult<TablespaceUsage[]>> {
    return this.api.get<TablespaceUsage[]>('/security/tablespaces');
  }

  create(request: CreateTablespaceRequest): Observable<OperationResult<string>> {
    return this.api.post<string>('/security/tablespaces', request);
  }

  /** The backend refuses the drop unless `confirm=true`, so the flag is always sent. */
  drop(name: string): Observable<OperationResult> {
    return this.api.delete(`/security/tablespaces/${enc(name)}?confirm=true`);
  }

  listDatafiles(tablespace: string): Observable<OperationResult<Datafile[]>> {
    return this.api.get<Datafile[]>(`/security/tablespaces/${enc(tablespace)}/datafiles`);
  }

  addDatafile(tablespace: string, fileName: string, sizeMb: number): Observable<OperationResult> {
    return this.api.post(`/security/tablespaces/${enc(tablespace)}/datafiles`, {
      fileName,
      sizeMb,
    });
  }

  resizeDatafile(tablespace: string, fileId: number, sizeMb: number): Observable<OperationResult> {
    return this.api.post(`/security/tablespaces/${enc(tablespace)}/datafiles/${fileId}/resize`, {
      sizeMb,
    });
  }
}
