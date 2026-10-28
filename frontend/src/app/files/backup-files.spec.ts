import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { BackupFiles } from './backup-files';

const DIRECTORIES = [
  { name: 'BACKUPS', path: 'C:\\backups' },
  { name: 'ARCHIVE', path: 'C:\\archive' },
];
const FILES = [
  { name: 'export.dmp', sizeBytes: 5 * 1024 * 1024, lastModified: '2026-10-27T10:00:00Z' },
  { name: 'export.log', sizeBytes: 2048, lastModified: '2026-10-27T10:05:00Z' },
];
const FILES_URL = '/api/files/directories/BACKUPS/files';

describe('BackupFiles', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let root: HTMLElement;

  // The actions resolve through promises, so let them settle before rendering.
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
  };
  const q = <T extends HTMLElement>(selector: string) => root.querySelector(selector) as T;
  const click = async (selector: string) => {
    q<HTMLButtonElement>(selector).click();
    await settle();
  };
  const flushDirectories = async () => {
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: DIRECTORIES });
    await settle();
  };
  const flushFiles = async (data: unknown = FILES) => {
    http.expectOne(FILES_URL).flush({ ok: true, message: 'ok', data });
    await settle();
  };
  const open = async (url: string) => {
    await harness.navigateByUrl(url, BackupFiles);
    root = harness.fixture.nativeElement;
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'files/browse', component: BackupFiles },
          { path: 'files/directories/:name/files', component: BackupFiles },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('asks for a directory when none is selected', async () => {
    await open('/files/browse');
    await flushDirectories();

    expect(root.textContent).toContain('Select a directory to see its files');
    expect(root.querySelector('table')).toBeNull();
  });

  it('lists the files of the directory in the URL with readable sizes', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    const rows = root.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('export.dmp');
    expect(rows[0].textContent).toContain('5.0 MB');
    expect(rows[1].textContent).toContain('2.0 KB');
  });

  it('says when the directory has no backup files', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles([]);

    expect(root.textContent).toContain('This directory has no .dmp or .log files');
  });

  it('shows the backend message when listing files fails', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    http
      .expectOne(FILES_URL)
      .flush({ ok: false, message: 'Directory BACKUPS does not exist', data: null }, {
        status: 400,
        statusText: 'Bad Request',
      });
    await settle();

    expect(q('.error').textContent).toContain('Directory BACKUPS does not exist');
    expect(root.querySelector('table')).toBeNull();
  });

  it('loads the files when a directory is chosen in the selector', async () => {
    await open('/files/browse');
    await flushDirectories();

    const select = q<HTMLSelectElement>('#directory');
    select.value = 'BACKUPS';
    // Browsers fire `input` (and then `change`) when a select value changes.
    select.dispatchEvent(new Event('input'));
    select.dispatchEvent(new Event('change'));
    await settle();

    // Choosing a directory navigates to its URL, which creates a fresh screen.
    await flushDirectories();
    await flushFiles();
    expect(root.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('offers to view only log files', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    expect(root.querySelector('[data-view="export.log"]')).not.toBeNull();
    expect(root.querySelector('[data-view="export.dmp"]')).toBeNull();
  });

  it('shows the content of a log file and warns when it is truncated', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    await click('[data-view="export.log"]');
    http
      .expectOne(`${FILES_URL}/export.log/content`)
      .flush({
        ok: true,
        message: 'ok',
        data: { name: 'export.log', content: 'Export: Release 21.0', truncated: true },
      });
    await settle();

    expect(q('pre.log').textContent).toContain('Export: Release 21.0');
    expect(root.textContent).toContain('Only the beginning of the file is shown');
  });

  it('asks for confirmation before deleting a file', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    await click('[data-delete="export.dmp"]');
    http.expectNone((r) => r.method === 'DELETE');
    expect(q('.confirm').textContent).toContain('export.dmp');

    await click('.confirm [data-action="confirm"]');
    const del = http.expectOne(`${FILES_URL}/export.dmp?confirm=true`);
    expect(del.request.method).toBe('DELETE');
    del.flush({ ok: true, message: 'File export.dmp deleted', data: null });
    await settle();
    await flushFiles([FILES[1]]);

    expect(q('.success').textContent).toContain('File export.dmp deleted');
    expect(root.querySelectorAll('tbody tr').length).toBe(1);
    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('cancels a delete without calling the backend', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    await click('[data-delete="export.dmp"]');
    await click('.confirm [data-action="cancel"]');

    http.expectNone((r) => r.method === 'DELETE');
    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('shows the backend message when a delete fails', async () => {
    await open('/files/directories/BACKUPS/files');
    await flushDirectories();
    await flushFiles();

    await click('[data-delete="export.dmp"]');
    await click('.confirm [data-action="confirm"]');
    http
      .expectOne(`${FILES_URL}/export.dmp?confirm=true`)
      .flush({ ok: false, message: 'Could not delete export.dmp', data: null }, {
        status: 500,
        statusText: 'Server Error',
      });
    await settle();

    expect(q('.error').textContent).toContain('Could not delete export.dmp');
  });
});
