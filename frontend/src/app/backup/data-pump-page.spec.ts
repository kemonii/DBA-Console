import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BackupApiService } from './backup-api.service';
import { DataPumpPage } from './data-pump-page';
import { DataPumpResult, OperationResult } from './backup.models';

const success: OperationResult<DataPumpResult> = {
  ok: true,
  message: 'Backup completed',
  data: { success: true, exitCode: 0, command: 'expdp system/******@//localhost:1521/XEPDB1', output: ['Job completed'] },
};

describe('DataPumpPage', () => {
  const api = { backup: vi.fn(), recover: vi.fn(), readLog: vi.fn() };
  let fixture: ComponentFixture<DataPumpPage>;
  let page: HTMLElement;

  function create(mode: 'backup' | 'recovery') {
    fixture = TestBed.createComponent(DataPumpPage);
    fixture.componentRef.setInput('mode', mode);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  }

  function field(name: string): HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement {
    return page.querySelector(`[name="${name}"]`) as HTMLInputElement;
  }

  function type(name: string, value: string) {
    const element = field(name);
    element.value = value;
    element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function check(name: string, checked: boolean) {
    const element = field(name) as HTMLInputElement;
    element.checked = checked;
    element.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  async function submit() {
    (page.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function fillSchemaBackup() {
    type('schema', 'hr');
    type('directory', 'RESPALDO');
    type('fileName', 'hr');
  }

  beforeEach(async () => {
    vi.resetAllMocks();
    api.backup.mockReturnValue(of(success));
    api.recover.mockReturnValue(of({ ...success, message: 'Recovery completed' }));
    await TestBed.configureTestingModule({
      imports: [DataPumpPage],
      providers: [{ provide: BackupApiService, useValue: api }],
    }).compileComponents();
  });

  it('titles the page after its mode', () => {
    create('backup');
    expect(page.querySelector('h2')?.textContent).toContain('Backup');

    create('recovery');
    expect(page.querySelector('h2')?.textContent).toContain('Recovery');
  });

  it('asks for the fields the selected scope needs', () => {
    create('backup');
    expect(field('schema')).not.toBeNull();
    expect(field('table')).toBeNull();

    type('type', 'TABLE');
    expect(field('schema')).not.toBeNull();
    expect(field('table')).not.toBeNull();

    type('type', 'FULL');
    expect(field('schema')).toBeNull();
    expect(field('table')).toBeNull();
  });

  it('sends the backup request and shows the command and the output', async () => {
    create('backup');
    fillSchemaBackup();

    await submit();

    expect(api.backup).toHaveBeenCalledWith({
      type: 'SCHEMA',
      schema: 'hr',
      directory: 'RESPALDO',
      fileName: 'hr',
    });
    expect(page.textContent).toContain('Backup completed');
    expect(page.querySelector('.command')?.textContent).toContain('expdp system/******@//localhost:1521/XEPDB1');
    expect(page.querySelector('.output')?.textContent).toContain('Job completed');
  });

  it('shows the message and the output of a failed run', async () => {
    api.backup.mockReturnValue(
      of({
        ok: false,
        message: 'Backup failed with exit code 1',
        data: { success: false, exitCode: 1, command: 'expdp ...', output: ['ORA-39002: invalid operation'] },
      }),
    );
    create('backup');
    fillSchemaBackup();

    await submit();

    expect(page.querySelector('.error')?.textContent).toContain('Backup failed with exit code 1');
    expect(page.querySelector('.output')?.textContent).toContain('ORA-39002');
  });

  it('shows the message of a request the server rejected', async () => {
    api.backup.mockReturnValue(of({ ok: false, message: 'Invalid Oracle identifier: x y', data: null }));
    create('backup');
    fillSchemaBackup();

    await submit();

    expect(page.querySelector('.error')?.textContent).toContain('Invalid Oracle identifier: x y');
    expect(page.querySelector('.command')).toBeNull();
  });

  it('does not call the API while required fields are missing', async () => {
    create('backup');
    type('directory', 'RESPALDO');

    await submit();

    expect(api.backup).not.toHaveBeenCalled();
    expect(page.querySelector('.error')?.textContent).toContain('Schema is required');
  });

  it('turns the table filter into a list of names for a backup', async () => {
    create('backup');
    fillSchemaBackup();
    type('filterKind', 'exclude');
    type('filterTables', 'audit_log, tmp_data');

    await submit();

    expect(api.backup).toHaveBeenCalledWith(
      expect.objectContaining({ excludeTables: ['audit_log', 'tmp_data'] }),
    );
  });

  it('does not offer table filters for a recovery or a table backup', () => {
    create('recovery');
    expect(field('filterKind')).toBeNull();

    create('backup');
    type('type', 'TABLE');
    expect(field('filterKind')).toBeNull();
  });

  it('warns about a full recovery and sends the confirmation', async () => {
    create('recovery');
    type('type', 'FULL');
    type('directory', 'RESPALDO');
    type('fileName', 'everything');

    expect(page.querySelector('.warning')?.textContent).toContain('overwrites the whole database');

    await submit();
    expect(api.recover).toHaveBeenCalledWith(expect.objectContaining({ type: 'FULL', confirmFull: false }));

    check('confirmFull', true);
    await submit();
    expect(api.recover).toHaveBeenLastCalledWith(expect.objectContaining({ confirmFull: true }));
  });

  it('shows the log of the run on demand', async () => {
    api.readLog.mockReturnValue(of({ ok: true, message: 'Contents of hr.log', data: ['Export started', 'Job completed'] }));
    create('backup');
    fillSchemaBackup();
    await submit();

    (page.querySelector('button.view-log') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.readLog).toHaveBeenCalledWith('RESPALDO', 'hr.log');
    expect(page.querySelector('.log')?.textContent).toContain('Export started');
  });

  it('reads the import log for a recovery', async () => {
    api.readLog.mockReturnValue(of({ ok: true, message: 'Contents of hr_imp.log', data: [] }));
    create('recovery');
    fillSchemaBackup();
    await submit();

    (page.querySelector('button.view-log') as HTMLButtonElement).click();

    expect(api.readLog).toHaveBeenCalledWith('RESPALDO', 'hr_imp.log');
  });

  it('shows why a log could not be read', async () => {
    api.readLog.mockReturnValue(
      of({ ok: false, message: 'Log file hr.log not found in directory RESPALDO', data: null }),
    );
    create('backup');
    fillSchemaBackup();
    await submit();

    (page.querySelector('button.view-log') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(page.querySelector('.log-error')?.textContent).toContain('not found in directory RESPALDO');
  });
});
