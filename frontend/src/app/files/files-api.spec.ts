import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FilesApi } from './files-api';

describe('FilesApi', () => {
  let api: FilesApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(FilesApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists directories with GET /api/files/directories', () => {
    api.listDirectories().subscribe();
    const req = http.expectOne('/api/files/directories');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'ok', data: [] });
  });

  it('creates a directory with POST and the name and path in the body', () => {
    api.createDirectory('BACKUPS', 'C:\backups').subscribe();
    const req = http.expectOne('/api/files/directories');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'BACKUPS', path: 'C:\backups' });
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('drops a directory with DELETE', () => {
    api.dropDirectory('BACKUPS').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('grants access with POST and the user in the body', () => {
    api.grant('BACKUPS', 'HR').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS/grants');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ user: 'HR' });
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('revokes access with DELETE', () => {
    api.revoke('BACKUPS', 'HR').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS/grants/HR');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('lists the files of a directory', () => {
    api.listFiles('BACKUPS').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS/files');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'ok', data: [] });
  });

  it('reads a log file', () => {
    api.readLog('BACKUPS', 'export.log').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS/files/export.log/content');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('deletes a file with the confirm flag', () => {
    api.deleteFile('BACKUPS', 'export.dmp').subscribe();
    const req = http.expectOne('/api/files/directories/BACKUPS/files/export.dmp?confirm=true');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ok: true, message: 'ok', data: null });
  });

  it('encodes path segments', () => {
    api.listFiles('A/B C').subscribe();
    const req = http.expectOne('/api/files/directories/A%2FB%20C/files');
    req.flush({ ok: true, message: 'ok', data: [] });
  });
});
