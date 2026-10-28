import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import {
  AuditOptions,
  AuditTrailParameter,
  ObjectEntry,
  SessionEntry,
  TrailEntry,
  Whenever,
} from './auditing.models';

const enc = encodeURIComponent;

/**
 * Calls the auditing endpoints (`/api/security/auditing/...`).
 * Every value that comes from the user is URL-encoded here, so screens pass raw values.
 */
@Service()
export class AuditingApi {
  private readonly api = inject(ApiClient);

  parameter(): Observable<OperationResult<AuditTrailParameter>> {
    return this.api.get<AuditTrailParameter>('/security/auditing/parameter');
  }

  options(): Observable<OperationResult<AuditOptions>> {
    return this.api.get<AuditOptions>('/security/auditing/options');
  }

  auditSessions(whenever: Whenever): Observable<OperationResult> {
    return this.api.post('/security/auditing/session', { whenever });
  }

  stopAuditingSessions(whenever: Whenever): Observable<OperationResult> {
    return this.api.delete(`/security/auditing/session?whenever=${whenever}`);
  }

  auditTable(
    owner: string,
    table: string,
    actions: string[],
    byAccess: boolean,
  ): Observable<OperationResult> {
    return this.api.post('/security/auditing/tables', { owner, table, actions, byAccess });
  }

  stopAuditingTable(owner: string, table: string, actions: string[]): Observable<OperationResult> {
    const query = `owner=${enc(owner)}&table=${enc(table)}&actions=${enc(actions.join(','))}`;
    return this.api.delete(`/security/auditing/tables?${query}`);
  }

  /** Login attempts (DBA_AUDIT_SESSION). An empty `username` means every user. */
  sessions(username: string, limit: number): Observable<OperationResult<SessionEntry[]>> {
    return this.api.get<SessionEntry[]>(
      `/security/auditing/trail/sessions?${filter(username, limit)}`,
    );
  }

  /** Audited actions on objects (DBA_AUDIT_OBJECT). */
  objects(username: string, limit: number): Observable<OperationResult<ObjectEntry[]>> {
    return this.api.get<ObjectEntry[]>(
      `/security/auditing/trail/objects?${filter(username, limit)}`,
    );
  }

  /** Every audited action (DBA_AUDIT_TRAIL). */
  trail(username: string, limit: number): Observable<OperationResult<TrailEntry[]>> {
    return this.api.get<TrailEntry[]>(`/security/auditing/trail?${filter(username, limit)}`);
  }
}

function filter(username: string, limit: number): string {
  return `limit=${limit}` + (username ? `&username=${enc(username)}` : '');
}
