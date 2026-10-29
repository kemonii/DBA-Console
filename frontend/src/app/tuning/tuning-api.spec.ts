import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TuningApi } from './tuning-api';
const OK = { ok: true, message: 'ok', data: [] };

describe('TuningApi', () => {
  let api: TuningApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(TuningApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('posts SQL as a body without altering it', () => {
    const statement = "select * from hr.jobs where job_id = 'IT_PROG'";
    api.explain(statement).subscribe();
    const request = http.expectOne('/api/tuning/queries/plan');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ statement });
    request.flush(OK);
  });
  it('gathers by schema or table and encodes names', () => {
    api.gather('HR', '').subscribe();
    api.gather('HR', 'JOBS#').subscribe();
    http.expectOne('/api/tuning/queries/statistics/HR/gather').flush(OK);
    http.expectOne('/api/tuning/queries/statistics/HR/JOBS%23/gather').flush(OK);
  });
  it('sends typed index columns and explicit deletion confirmation', () => {
    api.createIndex('HR', 'JOBS', 'IDX', ['JOB_ID'], 'BTREE').subscribe();
    const create = http.expectOne('/api/tuning/queries/indexes');
    expect(create.request.body.columns).toEqual(['JOB_ID']);
    create.flush(OK);
    api.drop('HR', 'IDX#').subscribe();
    const drop = http.expectOne('/api/tuning/queries/indexes/HR/IDX%23?confirm=true');
    expect(drop.request.method).toBe('DELETE');
    drop.flush(OK);
  });
  it('preserves SID and serial when terminating a blocker', () => {
    api.kill(42, 123).subscribe();
    const request = http.expectOne('/api/tuning/monitoring/sessions/42/123?confirm=true');
    expect(request.request.method).toBe('DELETE');
    request.flush(OK);
  });
});
