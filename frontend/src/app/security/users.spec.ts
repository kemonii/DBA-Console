import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Users } from './users';

const USERS = [
  {
    username: 'PRUEBA',
    accountStatus: 'OPEN',
    defaultTablespace: 'USERS',
    temporaryTablespace: 'TEMP',
    profile: 'DEFAULT',
    created: '2026-10-27T10:30:00',
  },
  {
    username: 'VIEJO',
    accountStatus: 'LOCKED',
    defaultTablespace: 'USERS',
    temporaryTablespace: 'TEMP',
    profile: 'DEFAULT',
    created: null,
  },
];
const TABLESPACES = [
  { name: 'USERS', totalMb: 5, usedMb: 1, freeMb: 4, usedPercent: 20, freePercent: 80 },
];
const OK = { ok: true, message: 'Done', data: null };

describe('Users', () => {
  let fixture: ComponentFixture<Users>;
  let http: HttpTestingController;
  let root: HTMLElement;

  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };
  const q = <T extends HTMLElement>(selector: string) => root.querySelector(selector) as T;
  const fill = async (selector: string, value: string) => {
    const el = q<HTMLInputElement | HTMLSelectElement>(selector);
    el.value = value;
    el.dispatchEvent(new Event('input'));
    el.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  };
  const click = async (selector: string) => {
    q<HTMLElement>(selector).click();
    await settle();
  };
  const loadUsers = async () => {
    http.expectOne('/api/security/users').flush({ ok: true, message: 'ok', data: USERS });
    await settle();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Users);
    root = fixture.nativeElement;
    await fixture.whenStable();
    http
      .expectOne('/api/security/tablespaces')
      .flush({ ok: true, message: 'ok', data: TABLESPACES });
  });

  afterEach(() => http.verify());

  it('lists the users and links to their privileges', async () => {
    await loadUsers();

    const rows = root.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('PRUEBA');
    expect(rows[1].querySelector('.status-locked')?.textContent).toContain('LOCKED');
    expect(rows[0].querySelector('a')?.getAttribute('href')).toBe(
      '/security/privileges?grantee=PRUEBA',
    );
  });

  it('offers the tablespaces as default tablespace', async () => {
    await loadUsers();

    const options = Array.from(q<HTMLSelectElement>('#create-default').options).map((o) => o.value);
    expect(options).toEqual(['', 'USERS']);
  });

  it('creates a user with a limited quota', async () => {
    await loadUsers();
    await fill('#create-username', 'nuevo');
    await fill('#create-password', 'Secreta1');
    await fill('#create-default', 'USERS');
    await fill('#create-quota-mode', 'limited');
    await fill('#create-quota', '50');

    await click('#create-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/security/users');
    expect(post.request.body).toEqual({
      username: 'nuevo',
      password: 'Secreta1',
      defaultTablespace: 'USERS',
      quotaMb: 50,
    });
    post.flush({ ok: true, message: 'User NUEVO created', data: 'NUEVO' });
    await settle();
    await loadUsers();

    expect(q('.success').textContent).toContain('User NUEVO created');
  });

  it('creates a user with an unlimited quota', async () => {
    await loadUsers();
    await fill('#create-username', 'nuevo');
    await fill('#create-password', 'Secreta1');
    await fill('#create-default', 'USERS');
    await fill('#create-quota-mode', 'unlimited');

    await click('#create-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/security/users');
    expect(post.request.body).toEqual({
      username: 'nuevo',
      password: 'Secreta1',
      defaultTablespace: 'USERS',
      unlimitedQuota: true,
    });
    post.flush(OK);
    await settle();
    await loadUsers();
  });

  it('requires a default tablespace for a quota', async () => {
    await loadUsers();
    await fill('#create-username', 'nuevo');
    await fill('#create-password', 'Secreta1');
    await fill('#create-quota-mode', 'unlimited');

    await click('#create-submit');

    http.expectNone((r) => r.method === 'POST');
    expect(q('.error').textContent).toContain('A quota needs a default tablespace');
  });

  it('rejects a password with double quotes before calling the backend', async () => {
    await loadUsers();
    await fill('#create-username', 'nuevo');
    await fill('#create-password', 'a"b');

    await click('#create-submit');

    http.expectNone((r) => r.method === 'POST');
    expect(q('.error').textContent).toContain('cannot contain double quotes');
  });

  it('locks an open user and unlocks a locked one', async () => {
    await loadUsers();

    await click('[data-lock="PRUEBA"]');
    http.expectOne('/api/security/users/PRUEBA/lock').flush(OK);
    await settle();
    await loadUsers();

    expect(q('[data-lock="VIEJO"]').textContent?.trim()).toBe('Unlock');
    await click('[data-lock="VIEJO"]');
    http.expectOne('/api/security/users/VIEJO/unlock').flush(OK);
    await settle();
    await loadUsers();
  });

  it('drops a user with CASCADE after confirmation', async () => {
    await loadUsers();

    await click('[data-drop="PRUEBA"]');
    http.expectNone((r) => r.method === 'DELETE');
    await click('#drop-cascade');
    await click('.confirm [data-action="confirm"]');

    const del = http.expectOne('/api/security/users/PRUEBA?cascade=true&confirm=true');
    expect(del.request.method).toBe('DELETE');
    del.flush(OK);
    await settle();
    await loadUsers();
  });

  it('cancels a drop without calling the backend', async () => {
    await loadUsers();

    await click('[data-drop="PRUEBA"]');
    await click('.confirm [data-action="cancel"]');

    http.expectNone((r) => r.method === 'DELETE');
    expect(root.querySelector('.confirm')).toBeNull();
  });
});
