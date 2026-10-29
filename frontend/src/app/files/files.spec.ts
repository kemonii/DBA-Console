import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { FILES_ROUTES } from './files.routes';

describe('File Management routes', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
  };
  const open = async (url: string) => {
    await harness.navigateByUrl(url);
    await settle();
  };
  const text = () => (harness.fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'files', children: FILES_ROUTES }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('redirects /files to the directories screen', async () => {
    await open('/files');
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: [] });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/files/directories');
    expect(text()).toContain('Objetos directorio');
  });

  it('shows the sub-navigation with both screens', async () => {
    await open('/files/directories');
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: [] });
    await settle();

    const links = Array.from(
      (harness.fixture.nativeElement as HTMLElement).querySelectorAll('nav a'),
    ).map((a) => a.textContent?.trim());
    expect(links).toEqual(['Directorios', 'Archivos de respaldo']);
  });

  it('serves the backup files screen at /files/browse', async () => {
    await open('/files/browse');
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: [] });
    await settle();

    expect(text()).toContain('Seleccione un directorio para ver sus archivos');
  });

  it('serves the files of a directory at /files/directories/:name/files', async () => {
    await open('/files/directories/BACKUPS/files');
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: [] });
    http
      .expectOne('/api/files/directories/BACKUPS/files')
      .flush({ ok: true, message: 'ok', data: [] });
    await settle();

    expect(text()).toContain('Este directorio no tiene archivos .dmp ni .log');
  });
});
