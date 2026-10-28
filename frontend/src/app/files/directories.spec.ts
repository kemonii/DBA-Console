import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Directories } from './directories';

const DIRECTORIES = [
  { name: 'BACKUPS', path: 'C:\backups' },
  { name: 'ARCHIVE', path: 'C:\archive' },
];
const OK = { ok: true, message: 'Done', data: null };

describe('Directories', () => {
  let fixture: ComponentFixture<Directories>;
  let http: HttpTestingController;
  let root: HTMLElement;

  // The submit actions resolve through promises, so let them settle before rendering.
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };
  const q = <T extends HTMLElement>(selector: string) => root.querySelector(selector) as T;
  const fill = async (selector: string, value: string) => {
    const el = q<HTMLInputElement | HTMLSelectElement>(selector);
    el.value = value;
    // Browsers fire `input` (and then `change`) when a select value changes.
    el.dispatchEvent(new Event('input'));
    el.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  };
  const click = async (selector: string) => {
    q<HTMLButtonElement>(selector).click();
    await settle();
  };
  const loadDirectories = async () => {
    http.expectOne('/api/files/directories').flush({ ok: true, message: 'ok', data: DIRECTORIES });
    await settle();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Directories);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('lists the directory objects with their paths', async () => {
    await loadDirectories();

    const rows = root.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('BACKUPS');
    expect(rows[0].textContent).toContain('C:\backups');
  });

  it('shows the backend message when listing fails', async () => {
    http
      .expectOne('/api/files/directories')
      .flush({ ok: false, message: 'ORA-01031: insufficient privileges', data: null }, {
        status: 500,
        statusText: 'Server Error',
      });
    await settle();

    expect(q('.error').textContent).toContain('ORA-01031');
    expect(root.querySelector('tbody')).toBeNull();
  });

  it('creates a directory and reloads the list', async () => {
    await loadDirectories();
    await fill('#create-name', 'DUMPS');
    await fill('#create-path', 'D:\dumps');

    await click('#create-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/files/directories');
    expect(post.request.body).toEqual({ name: 'DUMPS', path: 'D:\dumps' });
    post.flush({ ok: true, message: 'Directory DUMPS created', data: null });
    await settle();

    await loadDirectories();
    expect(q('.success').textContent).toContain('Directory DUMPS created');
  });

  it('does not call the backend when the create form is incomplete', async () => {
    await loadDirectories();

    await click('#create-submit');

    http.expectNone((r) => r.method === 'POST');
    expect(q('.error').textContent).toContain('Enter a directory name');
  });

  it('asks for confirmation before dropping a directory', async () => {
    await loadDirectories();

    await click('[data-drop="BACKUPS"]');
    http.expectNone((r) => r.method === 'DELETE');
    expect(q('.confirm').textContent).toContain('BACKUPS');

    await click('.confirm [data-action="confirm"]');
    const del = http.expectOne('/api/files/directories/BACKUPS');
    expect(del.request.method).toBe('DELETE');
    del.flush(OK);
    await settle();
    await loadDirectories();

    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('cancels a drop without calling the backend', async () => {
    await loadDirectories();

    await click('[data-drop="BACKUPS"]');
    await click('.confirm [data-action="cancel"]');

    http.expectNone((r) => r.method === 'DELETE');
    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('grants READ and WRITE on the selected directory to a user', async () => {
    await loadDirectories();
    await fill('#access-directory', 'BACKUPS');
    await fill('#access-user', 'HR');

    await click('#grant-submit');

    const post = http.expectOne('/api/files/directories/BACKUPS/grants');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ user: 'HR' });
    post.flush({ ok: true, message: 'Granted', data: null });
    await settle();
    expect(q('.success').textContent).toContain('Granted');
  });

  it('revokes access from a user', async () => {
    await loadDirectories();
    await fill('#access-directory', 'ARCHIVE');
    await fill('#access-user', 'HR');

    await click('#revoke-submit');

    const del = http.expectOne('/api/files/directories/ARCHIVE/grants/HR');
    expect(del.request.method).toBe('DELETE');
    del.flush({ ok: true, message: 'Revoked', data: null });
    await settle();
    expect(q('.success').textContent).toContain('Revoked');
  });

  it('shows the backend message when an action fails', async () => {
    await loadDirectories();
    await fill('#access-directory', 'BACKUPS');
    await fill('#access-user', 'NOBODY');

    await click('#grant-submit');
    http
      .expectOne('/api/files/directories/BACKUPS/grants')
      .flush({ ok: false, message: 'ORA-01917: user does not exist', data: null }, {
        status: 500,
        statusText: 'Server Error',
      });
    await settle();

    expect(q('.error').textContent).toContain('ORA-01917');
  });
});
