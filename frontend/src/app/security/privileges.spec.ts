import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Privileges } from './privileges';

const ROLES = [
  { name: 'VENTAS_ROL', oracleMaintained: false },
  { name: 'CONNECT', oracleMaintained: true },
  { name: 'DBA', oracleMaintained: true },
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
const PRIVILEGES = {
  grantee: 'PRUEBA',
  systemPrivileges: [{ privilege: 'CREATE SESSION', adminOption: false }],
  roles: [{ role: 'CONNECT', adminOption: false, defaultRole: true }],
  objectPrivileges: [
    { owner: 'HR', objectName: 'EMPLOYEES', privilege: 'SELECT', grantable: false },
  ],
};
const OK = { ok: true, message: 'Done', data: null };

describe('Privileges', () => {
  let fixture: ComponentFixture<Privileges>;
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
  const flushStartup = async () => {
    http.expectOne('/api/security/roles').flush({ ok: true, message: 'ok', data: ROLES });
    http
      .expectOne('/api/security/grants/system-privileges')
      .flush({ ok: true, message: 'ok', data: ['CREATE SESSION', 'CREATE TABLE'] });
    http.expectOne('/api/security/users').flush({ ok: true, message: 'ok', data: USERS });
    await settle();
  };
  const flushPrivileges = async () => {
    http
      .expectOne('/api/security/grants/PRUEBA')
      .flush({ ok: true, message: 'ok', data: PRIVILEGES });
    await settle();
  };

  const create = async (grantee: string | null) => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(grantee ? { grantee } : {}) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Privileges);
    root = fixture.nativeElement;
    await fixture.whenStable();
    await flushStartup();
  };

  afterEach(() => http.verify());

  it('lists the roles, separating the ones maintained by Oracle', async () => {
    await create(null);

    expect(q('table.custom-roles').textContent).toContain('VENTAS_ROL');
    expect(q('.oracle-roles').textContent).toContain('CONNECT, DBA');
    expect(root.querySelector('[data-drop-role="CONNECT"]')).toBeNull();
  });

  it('creates a role', async () => {
    await create(null);
    await fill('#role-name', 'COMPRAS_ROL');

    await click('#role-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url === '/api/security/roles');
    expect(post.request.body).toEqual({ name: 'COMPRAS_ROL' });
    post.flush({ ok: true, message: 'Role COMPRAS_ROL created', data: 'COMPRAS_ROL' });
    await settle();
    http.expectOne('/api/security/roles').flush({ ok: true, message: 'ok', data: ROLES });
    await settle();

    expect(q('.success').textContent).toContain('Role COMPRAS_ROL created');
  });

  it('asks for confirmation before dropping a role', async () => {
    await create(null);

    await click('[data-drop-role="VENTAS_ROL"]');
    http.expectNone((r) => r.method === 'DELETE');
    await click('.confirm [data-action="confirm"]');
    http.expectOne('/api/security/roles/VENTAS_ROL').flush(OK);
    await settle();
    http.expectOne('/api/security/roles').flush({ ok: true, message: 'ok', data: ROLES });
    await settle();
  });

  it('shows the privileges of the grantee given in the URL', async () => {
    await create('PRUEBA');
    await flushPrivileges();

    expect(q('table.system-privileges').textContent).toContain('CREATE SESSION');
    expect(q('table.granted-roles').textContent).toContain('CONNECT');
    expect(q('table.object-privileges').textContent).toContain('HR.EMPLOYEES');
  });

  it('shows the privileges of a grantee typed in the form', async () => {
    await create(null);
    await fill('#grantee', 'PRUEBA');

    await click('#show-submit');
    await flushPrivileges();

    expect(root.textContent).toContain('System privileges of PRUEBA');
  });

  it('grants a system privilege and reloads the privileges', async () => {
    await create('PRUEBA');
    await flushPrivileges();
    await fill('#grant-privilege', 'CREATE TABLE');

    await click('#grant-submit');
    const post = http.expectOne('/api/security/grants/system');
    expect(post.request.body).toEqual({
      privilege: 'CREATE TABLE',
      grantee: 'PRUEBA',
      adminOption: false,
    });
    post.flush({ ok: true, message: 'Privilege granted', data: null });
    await settle();
    await flushPrivileges();

    expect(q('.success').textContent).toContain('Privilege granted');
  });

  it('grants a role with admin option', async () => {
    await create('PRUEBA');
    await flushPrivileges();
    await fill('#grant-kind', 'role');
    await fill('#grant-role', 'VENTAS_ROL');
    q<HTMLInputElement>('#grant-option').click();
    await fixture.whenStable();

    await click('#grant-submit');
    const post = http.expectOne('/api/security/grants/role');
    expect(post.request.body).toEqual({ role: 'VENTAS_ROL', grantee: 'PRUEBA', adminOption: true });
    post.flush(OK);
    await settle();
    await flushPrivileges();
  });

  it('grants the checked privileges on an object', async () => {
    await create('PRUEBA');
    await flushPrivileges();
    await fill('#grant-kind', 'object');
    await fill('#grant-owner', 'HR');
    await fill('#grant-object', 'EMPLOYEES');
    await click('[data-object-privilege="INSERT"]');

    await click('#grant-submit');
    const post = http.expectOne('/api/security/grants/object');
    expect(post.request.body).toEqual({
      privileges: ['SELECT', 'INSERT'],
      owner: 'HR',
      object: 'EMPLOYEES',
      grantee: 'PRUEBA',
      grantOption: false,
    });
    post.flush(OK);
    await settle();
    await flushPrivileges();
  });

  it('does not grant on an object without privileges', async () => {
    await create('PRUEBA');
    await flushPrivileges();
    await fill('#grant-kind', 'object');
    await fill('#grant-owner', 'HR');
    await fill('#grant-object', 'EMPLOYEES');
    await click('[data-object-privilege="SELECT"]');

    await click('#grant-submit');

    http.expectNone('/api/security/grants/object');
    expect(q('.error').textContent).toContain('Select at least one object privilege');
  });

  it('revokes each kind of grant from its row', async () => {
    await create('PRUEBA');
    await flushPrivileges();

    await click('[data-revoke-system="CREATE SESSION"]');
    http
      .expectOne('/api/security/grants/system?privilege=CREATE%20SESSION&grantee=PRUEBA')
      .flush(OK);
    await settle();
    await flushPrivileges();

    await click('[data-revoke-role="CONNECT"]');
    http.expectOne('/api/security/grants/role?role=CONNECT&grantee=PRUEBA').flush(OK);
    await settle();
    await flushPrivileges();

    await click('[data-revoke-object="HR.EMPLOYEES:SELECT"]');
    http
      .expectOne(
        '/api/security/grants/object?privileges=SELECT&owner=HR&object=EMPLOYEES&grantee=PRUEBA',
      )
      .flush(OK);
    await settle();
    await flushPrivileges();
  });

  it('shows the backend message when a grant fails', async () => {
    await create('PRUEBA');
    await flushPrivileges();
    await fill('#grant-privilege', 'CREATE TABLE');

    await click('#grant-submit');
    http.expectOne('/api/security/grants/system').flush(
      { ok: false, message: 'ORA-01031: insufficient privileges', data: null },
      {
        status: 500,
        statusText: 'Server Error',
      },
    );
    await settle();

    expect(q('.error').textContent).toContain('ORA-01031');
  });
});
