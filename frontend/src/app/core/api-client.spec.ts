import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiClient } from './api-client';
import { ApiError } from './api-error';
import { OperationResult } from './operation-result.model';

describe('ApiClient', () => {
  let api: ApiClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('prefixes paths with /api and returns the OperationResult', () => {
    let result: OperationResult | undefined;
    api.get('/health').subscribe((r) => (result = r));

    const req = http.expectOne('/api/health');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'Database connection is available', data: null });

    expect(result).toEqual({ ok: true, message: 'Database connection is available', data: null });
  });

  it('sends the body on POST', () => {
    api.post('/things', { name: 'a' }).subscribe();

    const req = http.expectOne('/api/things');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'a' });
    req.flush({ ok: true, message: 'created', data: null });
  });

  it('uses the backend message when an error response is an OperationResult', () => {
    let error: unknown;
    api.get('/health').subscribe({ error: (e) => (error = e) });

    http
      .expectOne('/api/health')
      .flush(
        { ok: false, message: 'ORA-00942: table or view does not exist', data: null },
        { status: 500, statusText: 'Server Error' },
      );

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe('ORA-00942: table or view does not exist');
    expect((error as ApiError).status).toBe(500);
  });

  it('keeps the data of an error response so screens can show the details', () => {
    let error: unknown;
    api.post('/backup', {}).subscribe({ error: (e) => (error = e) });

    http
      .expectOne('/api/backup')
      .flush(
        { ok: false, message: 'Backup failed with exit code 1', data: { output: ['ORA-39002'] } },
        { status: 500, statusText: 'Server Error' },
      );

    expect((error as ApiError).data).toEqual({ output: ['ORA-39002'] });
  });

  it('has no data when the error response carries none', () => {
    let error: unknown;
    api.get('/health').subscribe({ error: (e) => (error = e) });

    http
      .expectOne('/api/health')
      .flush({ ok: false, message: 'down', data: null }, { status: 500, statusText: 'Server Error' });

    expect((error as ApiError).data).toBeNull();
  });

  it('falls back to a generic message when the error body is not an OperationResult', () => {
    let error: unknown;
    api.get('/health').subscribe({ error: (e) => (error = e) });

    http.expectOne('/api/health').flush('Bad gateway', { status: 502, statusText: 'Bad Gateway' });

    expect((error as ApiError).message).toBe('La solicitud falló con el estado 502');
    expect((error as ApiError).status).toBe(502);
  });

  it('reports an unreachable server with status 0', () => {
    let error: unknown;
    api.get('/health').subscribe({ error: (e) => (error = e) });

    http.expectOne('/api/health').error(new ProgressEvent('error'));

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(0);
    expect((error as ApiError).message).toContain('No se puede conectar con el servidor');
  });
});
