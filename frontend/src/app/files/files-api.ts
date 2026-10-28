import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import { BackupFile, DirectoryObject, LogContent } from './files.models';

const enc = encodeURIComponent;

/**
 * Calls the File Management endpoints (`/api/files/...`).
 * Every path segment that comes from the user is URL-encoded here, so screens pass raw values.
 */
@Service()
export class FilesApi {
  private readonly api = inject(ApiClient);

  listDirectories(): Observable<OperationResult<DirectoryObject[]>> {
    return this.api.get<DirectoryObject[]>('/files/directories');
  }

  createDirectory(name: string, path: string): Observable<OperationResult> {
    return this.api.post('/files/directories', { name, path });
  }

  dropDirectory(name: string): Observable<OperationResult> {
    return this.api.delete(`/files/directories/${enc(name)}`);
  }

  grant(directory: string, user: string): Observable<OperationResult> {
    return this.api.post(`/files/directories/${enc(directory)}/grants`, { user });
  }

  revoke(directory: string, user: string): Observable<OperationResult> {
    return this.api.delete(`/files/directories/${enc(directory)}/grants/${enc(user)}`);
  }

  listFiles(directory: string): Observable<OperationResult<BackupFile[]>> {
    return this.api.get<BackupFile[]>(`/files/directories/${enc(directory)}/files`);
  }

  readLog(directory: string, file: string): Observable<OperationResult<LogContent>> {
    return this.api.get<LogContent>(
      `/files/directories/${enc(directory)}/files/${enc(file)}/content`,
    );
  }

  /** The backend refuses the delete unless `confirm=true`, so the flag is always sent. */
  deleteFile(directory: string, file: string): Observable<OperationResult> {
    return this.api.delete(`/files/directories/${enc(directory)}/files/${enc(file)}?confirm=true`);
  }
}
