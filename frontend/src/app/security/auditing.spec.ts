import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Auditing } from './auditing';

const OPTIONS = {
  statementOptions: [
    { userName: null, option: 'CREATE SESSION', success: 'BY ACCESS', failure: 'BY ACCESS' },
  ],
  objectOptions: [
    {
      owner: 'HR',
      objectName: 'JOBS',
      objectType: 'TABLE',
      insert: 'A/A',
      update: '-/-',
      delete: 'A/A',
      select: '-/-',
    },
  ],
};
const OK = { ok: true, message: 'Done', data: null };

describe('Auditing', () => {
  let fixture: ComponentFixture<Auditing>;
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
  const loadSettings = async (value = 'DB', enabled = true) => {
    http
      .expectOne('/api/security/auditing/parameter')
      .flush({ ok: true, message: 'ok', data: { value, enabled } });
    http
      .expectOne('/api/security/auditing/options')
      .flush({ ok: true, message: 'ok', data: OPTIONS });
    await settle();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Auditing);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('shows the parameter and what is audited', async () => {
    await loadSettings();

    expect(q('.parameter').textContent).toContain('audit_trail = DB');
    expect(q('table.statement-options').textContent).toContain('CREATE SESSION');
    expect(q('table.object-options').textContent).toContain('HR.JOBS');
  });

  it('explains how to enable the trail when the parameter is NONE', async () => {
    await loadSettings('NONE', false);

    expect(q('.parameter').classList).toContain('error');
    expect(root.textContent).toContain('ALTER SYSTEM SET audit_trail = DB SCOPE = SPFILE');
  });

  it('enables session auditing for failed attempts only', async () => {
    await loadSettings();
    await fill('#session-whenever', 'NOT_SUCCESSFUL');

    await click('#session-enable');
    const post = http.expectOne('/api/security/auditing/session');
    expect(post.request.body).toEqual({ whenever: 'NOT_SUCCESSFUL' });
    post.flush({ ok: true, message: 'Session auditing enabled', data: null });
    await settle();
    await loadSettings();

    expect(q('.success').textContent).toContain('Session auditing enabled');
  });

  it('disables session auditing', async () => {
    await loadSettings();

    await click('#session-disable');
    http.expectOne('/api/security/auditing/session?whenever=ALWAYS').flush(OK);
    await settle();
    await loadSettings();
  });

  it('audits a table with the checked actions', async () => {
    await loadSettings();
    await fill('#table-owner', 'HR');
    await fill('#table-name', 'REGIONS');
    await click('[data-action-option="SELECT"]');

    await click('#table-submit');
    const post = http.expectOne('/api/security/auditing/tables');
    expect(post.request.body).toEqual({
      owner: 'HR',
      table: 'REGIONS',
      actions: ['INSERT', 'UPDATE', 'DELETE', 'SELECT'],
      byAccess: true,
    });
    post.flush(OK);
    await settle();
    await loadSettings();
  });

  it('stops auditing only the actions that are audited on a table', async () => {
    await loadSettings();

    await click('[data-stop="HR.JOBS"]');
    http
      .expectOne('/api/security/auditing/tables?owner=HR&table=JOBS&actions=INSERT%2CDELETE')
      .flush(OK);
    await settle();
    await loadSettings();
  });

  it('reads the login attempts', async () => {
    await loadSettings();
    await fill('#trail-user', 'PRUEBA');

    await click('#trail-submit');
    http.expectOne('/api/security/auditing/trail/sessions?limit=100&username=PRUEBA').flush({
      ok: true,
      message: 'ok',
      data: [
        {
          timestamp: '2026-10-27T21:05:00',
          logoffTime: null,
          osUsername: 'monica',
          username: 'PRUEBA',
          terminal: 'TUF',
          outcome: 'Failed',
        },
      ],
    });
    await settle();

    expect(q('table.trail').textContent).toContain('Failed');
  });

  it('reads every audited action', async () => {
    await loadSettings();
    await fill('#trail-view', 'all');
    await fill('#trail-limit', '20');

    await click('#trail-submit');
    http
      .expectOne('/api/security/auditing/trail?limit=20')
      .flush({ ok: true, message: 'ok', data: [] });
    await settle();

    expect(q('.trail-empty').textContent).toContain('No audited actions recorded');
  });

  it('shows the backend message when an action fails', async () => {
    await loadSettings();

    await click('#session-enable');
    http.expectOne('/api/security/auditing/session').flush(
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
