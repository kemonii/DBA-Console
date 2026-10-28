import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import { CreateUserRequest, DbRole, DbUser, GranteePrivileges } from './users.models';

const enc = encodeURIComponent;

/**
 * Calls the user, role and privilege endpoints (`/api/security/users`, `/roles` and `/grants`).
 * Every value that comes from the user is URL-encoded here, so screens pass raw values.
 */
@Service()
export class UsersApi {
  private readonly api = inject(ApiClient);

  listUsers(): Observable<OperationResult<DbUser[]>> {
    return this.api.get<DbUser[]>('/security/users');
  }

  createUser(request: CreateUserRequest): Observable<OperationResult<string>> {
    return this.api.post<string>('/security/users', request);
  }

  setLocked(username: string, locked: boolean): Observable<OperationResult> {
    return this.api.post(`/security/users/${enc(username)}/${locked ? 'lock' : 'unlock'}`, {});
  }

  /** The backend refuses the drop unless `confirm=true`, so the flag is always sent. */
  dropUser(username: string, cascade: boolean): Observable<OperationResult> {
    return this.api.delete(`/security/users/${enc(username)}?cascade=${cascade}&confirm=true`);
  }

  listRoles(): Observable<OperationResult<DbRole[]>> {
    return this.api.get<DbRole[]>('/security/roles');
  }

  createRole(name: string): Observable<OperationResult<string>> {
    return this.api.post<string>('/security/roles', { name });
  }

  dropRole(name: string): Observable<OperationResult> {
    return this.api.delete(`/security/roles/${enc(name)}`);
  }

  listSystemPrivileges(): Observable<OperationResult<string[]>> {
    return this.api.get<string[]>('/security/grants/system-privileges');
  }

  privilegesOf(grantee: string): Observable<OperationResult<GranteePrivileges>> {
    return this.api.get<GranteePrivileges>(`/security/grants/${enc(grantee)}`);
  }

  grantSystemPrivilege(
    privilege: string,
    grantee: string,
    adminOption: boolean,
  ): Observable<OperationResult> {
    return this.api.post('/security/grants/system', { privilege, grantee, adminOption });
  }

  revokeSystemPrivilege(privilege: string, grantee: string): Observable<OperationResult> {
    return this.api.delete(
      `/security/grants/system?privilege=${enc(privilege)}&grantee=${enc(grantee)}`,
    );
  }

  grantRole(role: string, grantee: string, adminOption: boolean): Observable<OperationResult> {
    return this.api.post('/security/grants/role', { role, grantee, adminOption });
  }

  revokeRole(role: string, grantee: string): Observable<OperationResult> {
    return this.api.delete(`/security/grants/role?role=${enc(role)}&grantee=${enc(grantee)}`);
  }

  grantObjectPrivileges(
    privileges: string[],
    owner: string,
    object: string,
    grantee: string,
    grantOption: boolean,
  ): Observable<OperationResult> {
    return this.api.post('/security/grants/object', {
      privileges,
      owner,
      object,
      grantee,
      grantOption,
    });
  }

  revokeObjectPrivileges(
    privileges: string[],
    owner: string,
    object: string,
    grantee: string,
  ): Observable<OperationResult> {
    const query = `privileges=${enc(privileges.join(','))}&owner=${enc(owner)}&object=${enc(object)}`;
    return this.api.delete(`/security/grants/object?${query}&grantee=${enc(grantee)}`);
  }
}
