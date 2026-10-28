import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TablespacesApi } from './tablespaces-api';

describe('TablespacesApi', () => {
  let api: TablespacesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(TablespacesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists tablespaces with GET /api/security/tablespaces', () => {
    api.list().subscribe();
    const req = http.expectOne('/api/security/tablespaces');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'ok', data: [] });
  });

  it('creates a tablespace with POST and the request in the body', () => {
    const request = { name: 'VENTAS', fileName: 'ventas01.dbf', sizeMb: 2, nextMb: 1 };
    api.create(request).subscribe();
    const req = http.expectOne('/api/security/tablespaces');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ ok: true, message: 'ok', data: 'VENTAS' });
  });

  it('drops a tablespace with DELETE and confirm=true', () => {
    api.drop('VENTAS').subscribe();
    const req = http.expectOne('/api/security/tablespaces/VENTAS?confirm=true');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('lists the datafiles of a tablespace', () => {
    api.listDatafiles('USERS').subscribe();
    const req = http.expectOne('/api/security/tablespaces/USERS/datafiles');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'ok', data: [] });
  });

  it('adds a datafile with POST and the file name and size in the body', () => {
    api.addDatafile('VENTAS', 'ventas02.dbf', 256).subscribe();
    const req = http.expectOne('/api/security/tablespaces/VENTAS/datafiles');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ fileName: 'ventas02.dbf', sizeMb: 256 });
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('resizes a datafile with POST to its resize URL', () => {
    api.resizeDatafile('VENTAS', 12, 5).subscribe();
    const req = http.expectOne('/api/security/tablespaces/VENTAS/datafiles/12/resize');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sizeMb: 5 });
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('URL-encodes the tablespace name', () => {
    api.listDatafiles('A/B').subscribe();
    http
      .expectOne('/api/security/tablespaces/A%2FB/datafiles')
      .flush({ ok: true, message: 'ok', data: [] });
  });
});
