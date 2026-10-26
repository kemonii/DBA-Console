import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Observable, firstValueFrom } from 'rxjs';
import { BackupApiService } from './backup-api.service';
import { DataPumpResult } from './backup.models';

describe('BackupApiService', () => {
  let api: BackupApiService;
  let http: HttpTestingController;

  const pumpResult: DataPumpResult = {
    success: true,
    exitCode: 0,
    command: 'expdp system/******@//localhost:1521/XEPDB1',
    output: ['Job completed'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(BackupApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts a backup request', async () => {
    const request = { type: 'SCHEMA' as const, schema: 'hr', directory: 'RESPALDO', fileName: 'hr' };
    const result = firstValueFrom(api.backup(request));

    const call = http.expectOne('/api/backup');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush({ ok: true, message: 'Backup completed', data: pumpResult });

    expect(await result).toEqual({ ok: true, message: 'Backup completed', data: pumpResult });
  });

  it('keeps the body of a failed backup so the output can be shown', async () => {
    const result = firstValueFrom(api.backup({ type: 'FULL', directory: 'RESPALDO', fileName: 'all' }));

    http.expectOne('/api/backup').flush(
      { ok: false, message: 'Backup failed with exit code 1', data: { ...pumpResult, success: false, exitCode: 1 } },
      { status: 500, statusText: 'Server Error' },
    );

    const outcome = await result;
    expect(outcome.ok).toBe(false);
    expect(outcome.message).toBe('Backup failed with exit code 1');
    expect(outcome.data?.exitCode).toBe(1);
  });

  it('turns a validation error into a failed result', async () => {
    const result = firstValueFrom(api.recover({ type: 'SCHEMA', schema: 'hr', directory: 'RESPALDO', fileName: 'hr' }));

    http.expectOne('/api/recovery').flush(
      { ok: false, message: 'Schema HR already exists; drop the user before recovering it', data: null },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(await result).toEqual({
      ok: false,
      message: 'Schema HR already exists; drop the user before recovering it',
      data: null,
    });
  });

  it('reports an unreachable server as a failed result', async () => {
    const result = firstValueFrom(api.backup({ type: 'FULL', directory: 'RESPALDO', fileName: 'all' }));

    http.expectOne('/api/backup').error(new ProgressEvent('error'), { status: 0 });

    expect(await result).toEqual({ ok: false, message: 'Could not reach the server', data: null });
  });

  it('posts the recovery, flashback table, flashback query and restore requests', async () => {
    const ignore = (observable: Observable<unknown>) => firstValueFrom(observable).catch(() => undefined);

    ignore(api.recover({ type: 'FULL', directory: 'RESPALDO', fileName: 'all', confirmFull: true }));
    expect(http.expectOne('/api/recovery').request.body.confirmFull).toBe(true);

    ignore(api.flashbackTable({ schema: 'hr', table: 'employees' }));
    expect(http.expectOne('/api/flashback/table').request.method).toBe('POST');

    const at = { schema: 'hr', table: 'employees', timestamp: '2026-10-26T10:15:00' };
    ignore(api.flashbackQuery(at));
    expect(http.expectOne('/api/flashback/query').request.body).toEqual(at);

    ignore(api.flashbackRestore(at));
    expect(http.expectOne('/api/flashback/restore').request.body).toEqual(at);
  });

  it('reads a log file with the directory and file name as query parameters', async () => {
    const result = firstValueFrom(api.readLog('RESPALDO', 'hr.log'));

    const call = http.expectOne((r) => r.url === '/api/backup/log');
    expect(call.request.method).toBe('GET');
    expect(call.request.params.get('directory')).toBe('RESPALDO');
    expect(call.request.params.get('fileName')).toBe('hr.log');
    call.flush({ ok: true, message: 'Contents of hr.log', data: ['Export started'] });

    expect((await result).data).toEqual(['Export started']);
  });
});
