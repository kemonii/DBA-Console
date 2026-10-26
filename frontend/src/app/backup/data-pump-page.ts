import { Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { BackupApiService } from './backup-api.service';
import { BackupRequest, DataPumpResult, OperationResult, RecoveryRequest, Scope } from './backup.models';

type Mode = 'backup' | 'recovery';
type FilterKind = 'none' | 'include' | 'exclude';

/**
 * Form shared by the backup and recovery screens: both take a scope, a directory object and a file name. The
 * differences are the table filters (backup only) and the full-recovery confirmation (recovery only).
 */
@Component({
  selector: 'app-data-pump-page',
  imports: [ReactiveFormsModule],
  templateUrl: './data-pump-page.html',
  styleUrl: './data-pump-page.css',
})
export class DataPumpPage {
  readonly mode = input.required<Mode>();

  private readonly api = inject(BackupApiService);

  protected readonly form = new FormGroup({
    type: new FormControl<Scope>('SCHEMA', { nonNullable: true }),
    schema: new FormControl('', { nonNullable: true }),
    table: new FormControl('', { nonNullable: true }),
    directory: new FormControl('RESPALDO', { nonNullable: true }),
    fileName: new FormControl('', { nonNullable: true }),
    filterKind: new FormControl<FilterKind>('none', { nonNullable: true }),
    filterTables: new FormControl('', { nonNullable: true }),
    confirmFull: new FormControl(false, { nonNullable: true }),
  });

  protected readonly scope = toSignal(this.form.controls.type.valueChanges, { initialValue: 'SCHEMA' as Scope });
  protected readonly filterKind = toSignal(this.form.controls.filterKind.valueChanges, {
    initialValue: 'none' as FilterKind,
  });

  protected readonly title = computed(() => (this.mode() === 'backup' ? 'Backup' : 'Recovery'));
  protected readonly canFilter = computed(() => this.mode() === 'backup' && this.scope() !== 'TABLE');
  protected readonly isFullRecovery = computed(() => this.mode() === 'recovery' && this.scope() === 'FULL');

  protected readonly validation = signal<string | null>(null);
  protected readonly result = signal<OperationResult<DataPumpResult> | null>(null);
  protected readonly busy = signal(false);
  protected readonly logLines = signal<string[] | null>(null);
  protected readonly logError = signal<string | null>(null);

  /** Directory and file name of the last run, so "View log" shows the log of what was just executed. */
  private lastRun: { directory: string; logFile: string } | null = null;

  protected submit(): void {
    const problem = this.missingField();
    this.validation.set(problem);
    if (problem) {
      return;
    }

    const value = this.form.getRawValue();
    const request = this.buildRequest();
    const logFile = this.mode() === 'backup' ? `${value.fileName}.log` : `${value.fileName}_imp.log`;

    this.busy.set(true);
    this.logLines.set(null);
    this.logError.set(null);
    const run =
      this.mode() === 'backup'
        ? this.api.backup(request)
        : this.api.recover({ ...request, confirmFull: value.confirmFull } satisfies RecoveryRequest);
    run.pipe(finalize(() => this.busy.set(false))).subscribe((result) => {
      this.result.set(result);
      this.lastRun = { directory: value.directory, logFile };
    });
  }

  protected viewLog(): void {
    if (!this.lastRun) {
      return;
    }
    this.logLines.set(null);
    this.logError.set(null);
    this.api.readLog(this.lastRun.directory, this.lastRun.logFile).subscribe((result) => {
      if (result.ok) {
        this.logLines.set(result.data ?? []);
      } else {
        this.logError.set(result.message);
      }
    });
  }

  private missingField(): string | null {
    const value = this.form.getRawValue();
    if (value.type !== 'FULL' && !value.schema.trim()) {
      return 'Schema is required';
    }
    if (value.type === 'TABLE' && !value.table.trim()) {
      return 'Table is required';
    }
    if (!value.directory.trim()) {
      return 'Directory is required';
    }
    if (!value.fileName.trim()) {
      return 'File name is required';
    }
    return null;
  }

  private buildRequest(): BackupRequest {
    const value = this.form.getRawValue();
    const request: BackupRequest = {
      type: value.type,
      directory: value.directory.trim(),
      fileName: value.fileName.trim(),
    };
    if (value.type !== 'FULL') {
      request.schema = value.schema.trim();
    }
    if (value.type === 'TABLE') {
      request.table = value.table.trim();
    }
    const tables = value.filterTables
      .split(',')
      .map((name) => name.trim())
      .filter((name) => name.length > 0);
    if (this.canFilter() && value.filterKind !== 'none' && tables.length > 0) {
      if (value.filterKind === 'include') {
        request.includeTables = tables;
      } else {
        request.excludeTables = tables;
      }
    }
    return request;
  }
}
