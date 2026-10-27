import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { DataPumpPage } from './data-pump-page';

const runResult = (command: string, output: string[]) => ({
  ok: true,
  message: 'Backup completed',
  data: { success: true, exitCode: 0, command, output },
});

describe('DataPumpPage', () => {
  let fixture: ComponentFixture<DataPumpPage>;
  let http: HttpTestingController;
  let root: HTMLElement;

  // Actions resolve through promises, so let them settle before rendering.
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  };

  async function create(mode: 'backup' | 'recovery') {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { mode } } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DataPumpPage);
    root = fixture.nativeElement;
    await fixture.whenStable();
  }

  const field = (id: string) => root.querySelector(`#${id}`) as HTMLInputElement;

  async function type(id: string, value: string) {
    const element = field(id);
    element.value = value;
    element.dispatchEvent(new Event('input'));
    element.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  async function check(id: string, checked: boolean) {
    const element = field(id);
    element.checked = checked;
    element.dispatchEvent(new Event('input'));
    element.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  async function fillSchemaBackup() {
    await type('schema', 'hr');
    await type('fileName', 'hr');
  }

  async function submit() {
    (root.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('titles the page after its mode', async () => {
    await create('backup');
    expect(root.querySelector('h2')?.textContent).toContain('Backup');
  });

  it('titles a recovery page accordingly', async () => {
    await create('recovery');
    expect(root.querySelector('h2')?.textContent).toContain('Recovery');
  });

  it('asks for the fields the selected scope needs', async () => {
    await create('backup');
    expect(field('schema')).not.toBeNull();
    expect(field('table')).toBeNull();

    await type('type', 'TABLE');
    expect(field('schema')).not.toBeNull();
    expect(field('table')).not.toBeNull();

    await type('type', 'FULL');
    expect(field('schema')).toBeNull();
    expect(field('table')).toBeNull();
  });

  it('sends the backup request and shows the command and the output', async () => {
    await create('backup');
    await fillSchemaBackup();

    await submit();
    const req = http.expectOne('/api/backup');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      type: 'SCHEMA',
      schema: 'hr',
      directory: 'RESPALDO',
      fileName: 'hr',
    });
    req.flush(runResult('expdp system/******@//localhost:1521/XEPDB1', ['Job completed']));
    await settle();

    expect(root.querySelector('.success')?.textContent).toContain('Backup completed');
    expect(root.querySelector('.command')?.textContent).toContain('expdp system/******@//localhost:1521/XEPDB1');
    expect(root.querySelector('.output')?.textContent).toContain('Job completed');
  });

  it('tells the user that a run can take a while, only while it is running', async () => {
    await create('backup');
    await fillSchemaBackup();

    await submit();
    expect(root.querySelector('.running')?.textContent).toContain('can take a minute');

    http.expectOne('/api/backup').flush(runResult('expdp', []));
    await settle();
    expect(root.querySelector('.running')).toBeNull();
  });

  it('shows the message and the output of a failed run', async () => {
    await create('backup');
    await fillSchemaBackup();

    await submit();
    http.expectOne('/api/backup').flush(
      {
        ok: false,
        message: 'Backup failed with exit code 1',
        data: { success: false, exitCode: 1, command: 'expdp ...', output: ['ORA-39002: invalid operation'] },
      },
      { status: 500, statusText: 'Server Error' },
    );
    await settle();

    expect(root.querySelector('.error')?.textContent).toContain('Backup failed with exit code 1');
    expect(root.querySelector('.output')?.textContent).toContain('ORA-39002');
  });

  it('shows the message of a request the server rejected', async () => {
    await create('backup');
    await fillSchemaBackup();

    await submit();
    http
      .expectOne('/api/backup')
      .flush(
        { ok: false, message: 'Invalid Oracle identifier: x y', data: null },
        { status: 400, statusText: 'Bad Request' },
      );
    await settle();

    expect(root.querySelector('.error')?.textContent).toContain('Invalid Oracle identifier: x y');
    expect(root.querySelector('.command')).toBeNull();
  });

  it('does not call the API while required fields are missing', async () => {
    await create('backup');

    await submit();

    http.expectNone('/api/backup');
    expect(root.querySelector('.error')?.textContent).toContain('Schema is required');
  });

  it('turns the table filter into a list of names for a backup', async () => {
    await create('backup');
    await fillSchemaBackup();
    await type('filterKind', 'exclude');
    await type('filterTables', 'audit_log, tmp_data');

    await submit();

    const req = http.expectOne('/api/backup');
    expect(req.request.body.excludeTables).toEqual(['audit_log', 'tmp_data']);
    expect(req.request.body.includeTables).toBeUndefined();
    req.flush(runResult('expdp', []));
    await settle();
  });

  it('does not offer table filters for a recovery or a table backup', async () => {
    await create('backup');
    expect(field('filterKind')).not.toBeNull();
    await type('type', 'TABLE');
    expect(field('filterKind')).toBeNull();
  });

  it('does not offer table filters in a recovery', async () => {
    await create('recovery');
    expect(field('filterKind')).toBeNull();
  });

  it('warns about a full recovery and sends the confirmation', async () => {
    await create('recovery');
    await type('type', 'FULL');
    await type('fileName', 'everything');

    expect(root.querySelector('.warning')?.textContent).toContain('overwrites the whole database');

    await submit();
    let req = http.expectOne('/api/recovery');
    expect(req.request.body).toEqual({
      type: 'FULL',
      directory: 'RESPALDO',
      fileName: 'everything',
      confirmFull: false,
    });
    req.flush(
      { ok: false, message: 'Full recovery overwrites the whole database. Send confirmFull=true to run it', data: null },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    await check('confirmFull', true);
    await submit();
    req = http.expectOne('/api/recovery');
    expect(req.request.body.confirmFull).toBe(true);
    req.flush(runResult('impdp', []));
    await settle();
  });

  it('shows the log of the run on demand', async () => {
    await create('backup');
    await fillSchemaBackup();
    await submit();
    http.expectOne('/api/backup').flush(runResult('expdp', ['Job completed']));
    await settle();

    (root.querySelector('button.view-log') as HTMLButtonElement).click();
    await fixture.whenStable();
    const req = http.expectOne('/api/backup/log?directory=RESPALDO&fileName=hr.log');
    expect(req.request.method).toBe('GET');
    req.flush({ ok: true, message: 'Contents of hr.log', data: ['Export started', 'Job completed'] });
    await settle();

    expect(root.querySelector('.log')?.textContent).toContain('Export started');
  });

  it('reads the import log for a recovery', async () => {
    await create('recovery');
    await fillSchemaBackup();
    await submit();
    http.expectOne('/api/recovery').flush(runResult('impdp', []));
    await settle();

    (root.querySelector('button.view-log') as HTMLButtonElement).click();
    await fixture.whenStable();

    http
      .expectOne('/api/backup/log?directory=RESPALDO&fileName=hr_imp.log')
      .flush({ ok: true, message: 'Contents of hr_imp.log', data: [] });
    await settle();
  });

  it('shows why a log could not be read', async () => {
    await create('backup');
    await fillSchemaBackup();
    await submit();
    http.expectOne('/api/backup').flush(runResult('expdp', []));
    await settle();

    (root.querySelector('button.view-log') as HTMLButtonElement).click();
    await fixture.whenStable();
    http
      .expectOne('/api/backup/log?directory=RESPALDO&fileName=hr.log')
      .flush(
        { ok: false, message: 'Log file hr.log not found in directory RESPALDO', data: null },
        { status: 400, statusText: 'Bad Request' },
      );
    await settle();

    expect(root.querySelector('.log-error')?.textContent).toContain('not found in directory RESPALDO');
  });
});
