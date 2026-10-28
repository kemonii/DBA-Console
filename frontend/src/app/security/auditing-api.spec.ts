import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuditingApi } from './auditing-api';

const OK = { ok: true, message: 'ok', data: null };

describe('AuditingApi', () => {
  let api: AuditingApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AuditingApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the parameter and the options', () => {
    api.parameter().subscribe();
    api.options().subscribe();
    http.expectOne('/api/security/auditing/parameter').flush(OK);
    http.expectOne('/api/security/auditing/options').flush(OK);
  });

  it('enables and disables session auditing', () => {
    api.auditSessions('NOT_SUCCESSFUL').subscribe();
    api.stopAuditingSessions('ALWAYS').subscribe();
    const post = http.expectOne('/api/security/auditing/session');
    expect(post.request.body).toEqual({ whenever: 'NOT_SUCCESSFUL' });
    post.flush(OK);
    const del = http.expectOne('/api/security/auditing/session?whenever=ALWAYS');
    expect(del.request.method).toBe('DELETE');
    del.flush(OK);
  });

  it('enables and disables table auditing', () => {
    api.auditTable('HR', 'JOBS', ['INSERT', 'UPDATE'], true).subscribe();
    api.stopAuditingTable('HR', 'JOBS', ['INSERT', 'UPDATE']).subscribe();
    expect(http.expectOne('/api/security/auditing/tables').request.body).toEqual({
      owner: 'HR',
      table: 'JOBS',
      actions: ['INSERT', 'UPDATE'],
      byAccess: true,
    });
    http.expectOne('/api/security/auditing/tables?owner=HR&table=JOBS&actions=INSERT%2CUPDATE');
    http.match(() => true).forEach((req) => req.flush(OK));
  });

  it('reads the three trail views, with the user filter only when given', () => {
    api.sessions('', 50).subscribe();
    api.objects('PRUEBA', 10).subscribe();
    api.trail('', 100).subscribe();
    http.expectOne('/api/security/auditing/trail/sessions?limit=50').flush(OK);
    http.expectOne('/api/security/auditing/trail/objects?limit=10&username=PRUEBA').flush(OK);
    http.expectOne('/api/security/auditing/trail?limit=100').flush(OK);
  });
});
