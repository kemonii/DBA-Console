import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UsersApi } from './users-api';

const OK = { ok: true, message: 'ok', data: null };

describe('UsersApi', () => {
  let api: UsersApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(UsersApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a user with POST and the request in the body', () => {
    const request = {
      username: 'PRUEBA',
      password: 'Secreta1',
      quotaMb: 50,
      defaultTablespace: 'USERS',
    };
    api.createUser(request).subscribe();
    const req = http.expectOne('/api/security/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(OK);
  });

  it('locks and unlocks a user', () => {
    api.setLocked('PRUEBA', true).subscribe();
    api.setLocked('PRUEBA', false).subscribe();
    expect(http.expectOne('/api/security/users/PRUEBA/lock').request.method).toBe('POST');
    expect(http.expectOne('/api/security/users/PRUEBA/unlock').request.method).toBe('POST');
    http.match(() => true).forEach((req) => req.flush(OK));
  });

  it('drops a user with confirm=true and the cascade flag', () => {
    api.dropUser('PRUEBA', true).subscribe();
    const req = http.expectOne('/api/security/users/PRUEBA?cascade=true&confirm=true');
    expect(req.request.method).toBe('DELETE');
    req.flush(OK);
  });

  it('creates and drops roles', () => {
    api.createRole('VENTAS_ROL').subscribe();
    api.dropRole('VENTAS_ROL').subscribe();
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/security/roles');
    expect(post.request.body).toEqual({ name: 'VENTAS_ROL' });
    post.flush(OK);
    http.expectOne('/api/security/roles/VENTAS_ROL').flush(OK);
  });

  it('reads the privileges of a grantee', () => {
    api.privilegesOf('PRUEBA').subscribe();
    const req = http.expectOne('/api/security/grants/PRUEBA');
    expect(req.request.method).toBe('GET');
    req.flush(OK);
  });

  it('grants and revokes a system privilege, encoding the space in the name', () => {
    api.grantSystemPrivilege('CREATE SESSION', 'PRUEBA', true).subscribe();
    api.revokeSystemPrivilege('CREATE SESSION', 'PRUEBA').subscribe();
    const post = http.expectOne('/api/security/grants/system');
    expect(post.request.body).toEqual({
      privilege: 'CREATE SESSION',
      grantee: 'PRUEBA',
      adminOption: true,
    });
    post.flush(OK);
    const del = http.expectOne(
      '/api/security/grants/system?privilege=CREATE%20SESSION&grantee=PRUEBA',
    );
    expect(del.request.method).toBe('DELETE');
    del.flush(OK);
  });

  it('grants and revokes a role', () => {
    api.grantRole('CONNECT', 'PRUEBA', false).subscribe();
    api.revokeRole('CONNECT', 'PRUEBA').subscribe();
    expect(http.expectOne('/api/security/grants/role').request.body).toEqual({
      role: 'CONNECT',
      grantee: 'PRUEBA',
      adminOption: false,
    });
    http.expectOne('/api/security/grants/role?role=CONNECT&grantee=PRUEBA');
    http.match(() => true).forEach((req) => req.flush(OK));
  });

  it('grants and revokes object privileges', () => {
    api.grantObjectPrivileges(['SELECT', 'INSERT'], 'HR', 'EMPLOYEES', 'PRUEBA', false).subscribe();
    api.revokeObjectPrivileges(['SELECT', 'INSERT'], 'HR', 'EMPLOYEES', 'PRUEBA').subscribe();
    expect(http.expectOne('/api/security/grants/object').request.body).toEqual({
      privileges: ['SELECT', 'INSERT'],
      owner: 'HR',
      object: 'EMPLOYEES',
      grantee: 'PRUEBA',
      grantOption: false,
    });
    http.expectOne(
      '/api/security/grants/object?privileges=SELECT%2CINSERT&owner=HR&object=EMPLOYEES&grantee=PRUEBA',
    );
    http.match(() => true).forEach((req) => req.flush(OK));
  });
});
