import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { OperationResult } from '../core/operation-result.model';
import { DbProfile } from './profiles.models';

const enc = encodeURIComponent;

/**
 * Calls the profile endpoints (`/api/security/profiles`) and the one that assigns a profile to a
 * user. Every path segment that comes from the user is URL-encoded here.
 */
@Service()
export class ProfilesApi {
  private readonly api = inject(ApiClient);

  list(): Observable<OperationResult<DbProfile[]>> {
    return this.api.get<DbProfile[]>('/security/profiles');
  }

  /** `limits` maps a resource name to a whole number, `UNLIMITED` or `DEFAULT`. */
  create(name: string, limits: Record<string, string>): Observable<OperationResult<string>> {
    return this.api.post<string>('/security/profiles', { name, limits });
  }

  alter(name: string, limits: Record<string, string>): Observable<OperationResult> {
    return this.api.put(`/security/profiles/${enc(name)}`, { limits });
  }

  drop(name: string): Observable<OperationResult> {
    return this.api.delete(`/security/profiles/${enc(name)}`);
  }

  assign(username: string, profile: string): Observable<OperationResult> {
    return this.api.put(`/security/users/${enc(username)}/profile`, { profile });
  }
}
