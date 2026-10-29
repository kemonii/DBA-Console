import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Profiles } from './profiles';
import { ProfilesApi } from './profiles-api';

const PROFILES = [
  {
    name: 'APP_USER',
    limits: [
      { resource: 'SESSIONS_PER_USER', resourceType: 'KERNEL', limit: '2' },
      { resource: 'FAILED_LOGIN_ATTEMPTS', resourceType: 'PASSWORD', limit: '3' },
    ],
  },
  {
    name: 'DEFAULT',
    limits: [
      { resource: 'SESSIONS_PER_USER', resourceType: 'KERNEL', limit: 'UNLIMITED' },
      { resource: 'FAILED_LOGIN_ATTEMPTS', resourceType: 'PASSWORD', limit: '10' },
    ],
  },
];
const USERS = [
  {
    username: 'PRUEBA',
    accountStatus: 'OPEN',
    defaultTablespace: 'USERS',
    temporaryTablespace: 'TEMP',
    profile: 'DEFAULT',
    created: null,
  },
];
const OK = { ok: true, message: 'Done', data: null };

describe('Profiles', () => {
  let fixture: ComponentFixture<Profiles>;
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
  const loadProfiles = async () => {
    http.expectOne('/api/security/profiles').flush({ ok: true, message: 'ok', data: PROFILES });
    await settle();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Profiles);
    root = fixture.nativeElement;
    await fixture.whenStable();
    http.expectOne('/api/security/users').flush({ ok: true, message: 'ok', data: USERS });
  });

  afterEach(() => http.verify());

  it('compares the limits of every profile, kernel limits first', async () => {
    await loadProfiles();

    const rows = Array.from(root.querySelectorAll('table.profiles tbody tr'));
    expect(rows.map((row) => row.querySelector('th')?.textContent?.trim())).toEqual([
      'SESSIONS_PER_USER',
      'FAILED_LOGIN_ATTEMPTS',
    ]);
    expect(rows[0].textContent).toContain('2');
    expect(rows[0].textContent).toContain('UNLIMITED');
  });

  it('offers to drop every profile except DEFAULT', async () => {
    await loadProfiles();

    expect(root.querySelector('[data-drop="APP_USER"]')).not.toBeNull();
    expect(root.querySelector('[data-drop="DEFAULT"]')).toBeNull();
  });

  it('creates a profile with only the filled limits', async () => {
    await loadProfiles();
    await fill('#edit-name', 'SOPORTE');
    await fill('#limit-SESSIONS_PER_USER', '2');
    await fill('#limit-IDLE_TIME', 'unlimited');

    await click('#edit-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/security/profiles');
    expect(post.request.body).toEqual({
      name: 'SOPORTE',
      limits: { SESSIONS_PER_USER: '2', IDLE_TIME: 'UNLIMITED' },
    });
    post.flush({ ok: true, message: 'Profile SOPORTE created', data: 'SOPORTE' });
    await settle();
    await loadProfiles();

    expect(q('.success').textContent).toContain('Profile SOPORTE created');
  });

  it('changes the DEFAULT profile as in class', async () => {
    await loadProfiles();
    await fill('#edit-target', 'DEFAULT');
    await fill('#limit-IDLE_TIME', '20');

    await click('#edit-submit');
    const put = http.expectOne('/api/security/profiles/DEFAULT');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ limits: { IDLE_TIME: '20' } });
    put.flush(OK);
    await settle();
    await loadProfiles();
  });

  it('rejects invalid limits and an empty form before calling the backend', async () => {
    await loadProfiles();
    await fill('#edit-name', 'SOPORTE');

    await click('#edit-submit');
    expect(q('.error').textContent).toContain('Establezca al menos un límite');

    await fill('#limit-IDLE_TIME', '20M');
    await click('#edit-submit');
    expect(q('.error').textContent).toContain('IDLE_TIME debe ser un número entero');

    http.expectNone((r) => r.method !== 'GET');
  });

  it('drops a profile after confirmation', async () => {
    await loadProfiles();

    await click('[data-drop="APP_USER"]');
    http.expectNone((r) => r.method === 'DELETE');
    await click('.confirm [data-action="confirm"]');
    http.expectOne('/api/security/profiles/APP_USER').flush(OK);
    await settle();
    await loadProfiles();
  });

  it('assigns a profile to a user', async () => {
    await loadProfiles();
    await fill('#assign-user', 'PRUEBA');
    await fill('#assign-profile', 'APP_USER');

    await click('#assign-submit');
    const put = http.expectOne('/api/security/users/PRUEBA/profile');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ profile: 'APP_USER' });
    put.flush({ ok: true, message: 'Profile assigned', data: null });
    await settle();

    expect(q('.success').textContent).toContain('Profile assigned');
  });
});

describe('ProfilesApi', () => {
  it('URL-encodes the profile and user names', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const api = TestBed.inject(ProfilesApi);
    const http = TestBed.inject(HttpTestingController);

    api.drop('A/B').subscribe();
    api.assign('X/Y', 'APP').subscribe();
    http.expectOne('/api/security/profiles/A%2FB').flush(OK);
    http.expectOne('/api/security/users/X%2FY/profile').flush(OK);
    http.verify();
  });
});
