import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Tablespaces } from './tablespaces';

const TABLESPACES = [
  { name: 'USERS', totalMb: 5, usedMb: 1.25, freeMb: 3.75, usedPercent: 25, freePercent: 75 },
  { name: 'VENTAS', totalMb: 2, usedMb: 1.8, freeMb: 0.2, usedPercent: 90, freePercent: 10 },
];
const DATAFILES = [
  {
    fileId: 12,
    fileName: 'C:/oradata/XEPDB1/ventas01.dbf',
    sizeMb: 2,
    autoextensible: true,
    maxSizeMb: 1024,
  },
];
const OK = { ok: true, message: 'Done', data: null };

describe('Tablespaces', () => {
  let fixture: ComponentFixture<Tablespaces>;
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
    el.dispatchEvent(new Event('input'));
    el.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  };
  const click = async (selector: string) => {
    q<HTMLButtonElement>(selector).click();
    await settle();
  };
  const loadTablespaces = async () => {
    http
      .expectOne('/api/security/tablespaces')
      .flush({ ok: true, message: 'ok', data: TABLESPACES });
    await settle();
  };
  const selectVentas = async () => {
    await click('[data-select="VENTAS"]');
    http
      .expectOne('/api/security/tablespaces/VENTAS/datafiles')
      .flush({ ok: true, message: 'ok', data: DATAFILES });
    await settle();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Tablespaces);
    root = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('lists the tablespaces with their space usage', async () => {
    await loadTablespaces();

    const rows = root.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('USERS');
    expect(rows[0].textContent).toContain('3.75');
    expect(rows[1].textContent).toContain('90%');
    expect(rows[1].querySelector('meter')?.value).toBe(90);
  });

  it('shows the backend message when listing fails', async () => {
    http.expectOne('/api/security/tablespaces').flush(
      { ok: false, message: 'ORA-01031: insufficient privileges', data: null },
      {
        status: 500,
        statusText: 'Server Error',
      },
    );
    await settle();

    expect(q('.error').textContent).toContain('ORA-01031');
    expect(root.querySelector('table')).toBeNull();
  });

  it('creates a tablespace and reloads the list', async () => {
    await loadTablespaces();
    await fill('#create-name', 'COMPRAS');
    await fill('#create-file', 'compras01.dbf');
    await fill('#create-size', '50');
    await fill('#create-next', '5');
    await fill('#create-max', '500');

    await click('#create-submit');
    const post = http.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/security/tablespaces',
    );
    expect(post.request.body).toEqual({
      name: 'COMPRAS',
      fileName: 'compras01.dbf',
      sizeMb: 50,
      nextMb: 5,
      maxSizeMb: 500,
    });
    post.flush({ ok: true, message: 'Tablespace COMPRAS created', data: 'COMPRAS' });
    await settle();

    await loadTablespaces();
    expect(q('.success').textContent).toContain('Tablespace COMPRAS created');
  });

  it('leaves the maximum size out when it is empty', async () => {
    await loadTablespaces();
    await fill('#create-name', 'COMPRAS');
    await fill('#create-file', 'compras01.dbf');

    await click('#create-submit');
    const post = http.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/security/tablespaces',
    );
    expect(post.request.body).toEqual({
      name: 'COMPRAS',
      fileName: 'compras01.dbf',
      sizeMb: 100,
      nextMb: 10,
    });
    post.flush(OK);
    await settle();
    await loadTablespaces();
  });

  it('does not call the backend when the create form is invalid', async () => {
    await loadTablespaces();
    await fill('#create-name', 'COMPRAS');
    await fill('#create-file', 'compras01.dbf');
    await fill('#create-size', 'ten');

    await click('#create-submit');

    http.expectNone((r) => r.method === 'POST');
    expect(q('.error').textContent).toContain('Use un número entero de MB');
  });

  it('asks for confirmation before dropping a tablespace', async () => {
    await loadTablespaces();

    await click('[data-drop="VENTAS"]');
    http.expectNone((r) => r.method === 'DELETE');
    expect(q('.confirm').textContent).toContain('VENTAS');
    expect(q('.confirm').textContent).toContain('no se puede deshacer');

    await click('.confirm [data-action="confirm"]');
    const del = http.expectOne('/api/security/tablespaces/VENTAS?confirm=true');
    expect(del.request.method).toBe('DELETE');
    del.flush(OK);
    await settle();
    await loadTablespaces();

    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('cancels a drop without calling the backend', async () => {
    await loadTablespaces();

    await click('[data-drop="VENTAS"]');
    await click('.confirm [data-action="cancel"]');

    http.expectNone((r) => r.method === 'DELETE');
    expect(root.querySelector('.confirm')).toBeNull();
  });

  it('shows the datafiles of the selected tablespace', async () => {
    await loadTablespaces();

    await selectVentas();

    expect(root.textContent).toContain('Datafiles de VENTAS');
    expect(q('table.datafiles').textContent).toContain('ventas01.dbf');
    expect(q('table.datafiles').textContent).toContain('Sí, hasta 1024 MB');
  });

  it('resizes a datafile and refreshes both lists', async () => {
    await loadTablespaces();
    await selectVentas();
    await fill('#resize-file', '12');
    await fill('#resize-size', '5');

    await click('#resize-submit');
    const post = http.expectOne('/api/security/tablespaces/VENTAS/datafiles/12/resize');
    expect(post.request.body).toEqual({ sizeMb: 5 });
    post.flush({ ok: true, message: 'Datafile resized', data: null });
    await settle();

    await loadTablespaces();
    http
      .expectOne('/api/security/tablespaces/VENTAS/datafiles')
      .flush({ ok: true, message: 'ok', data: DATAFILES });
    await settle();
    expect(q('.success').textContent).toContain('Datafile resized');
  });

  it('adds a datafile to the selected tablespace', async () => {
    await loadTablespaces();
    await selectVentas();
    await fill('#add-file', 'ventas02.dbf');
    await fill('#add-size', '256');

    await click('#add-submit');
    const post = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/VENTAS/datafiles'));
    expect(post.request.body).toEqual({ fileName: 'ventas02.dbf', sizeMb: 256 });
    post.flush({ ok: true, message: 'Datafile added', data: null });
    await settle();

    await loadTablespaces();
    http
      .expectOne('/api/security/tablespaces/VENTAS/datafiles')
      .flush({ ok: true, message: 'ok', data: DATAFILES });
    await settle();
    expect(q('.success').textContent).toContain('Datafile added');
  });

  it('shows the backend message when an action fails', async () => {
    await loadTablespaces();
    await selectVentas();
    await fill('#resize-file', '12');
    await fill('#resize-size', '1');

    await click('#resize-submit');
    http.expectOne('/api/security/tablespaces/VENTAS/datafiles/12/resize').flush(
      {
        ok: false,
        message: 'ORA-03297: file contains used data beyond requested RESIZE value',
        data: null,
      },
      {
        status: 500,
        statusText: 'Server Error',
      },
    );
    await settle();

    expect(q('.error').textContent).toContain('ORA-03297');
  });
});
