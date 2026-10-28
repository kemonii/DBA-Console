import { Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { BackupApi } from './backup-api';
import { BackupRequest, DataPumpResult, Scope } from './backup.models';

type Mode = 'backup' | 'recovery';
type FilterKind = 'none' | 'include' | 'exclude';

interface Outcome {
  ok: boolean;
  message: string;
}

/**
 * Form shared by the backup and recovery screens: both take a scope, a directory object and a file name. The
 * differences are the table filters (backup only) and the full-recovery confirmation (recovery only). The mode comes
 * from the route data.
 */
@Component({
  selector: 'app-data-pump-page',
  imports: [FormField],
  templateUrl: './data-pump-page.html',
  styleUrl: './data-pump-page.css',
})
export class DataPumpPage {
  private readonly api = inject(BackupApi);

  protected readonly mode: Mode = inject(ActivatedRoute).snapshot.data['mode'];
  protected readonly title = this.mode === 'backup' ? 'Backup' : 'Recovery';

  protected readonly model = signal({
    type: 'SCHEMA' as Scope,
    schema: '',
    table: '',
    directory: 'RESPALDO',
    fileName: '',
    filterKind: 'none' as FilterKind,
    filterTables: '',
    confirmFull: false,
  });

  protected readonly dataForm = form(this.model, (schema) => {
    required(schema.schema, {
      message: 'Schema is required',
      when: ({ valueOf }) => valueOf(schema.type) !== 'FULL',
    });
    required(schema.table, {
      message: 'Table is required',
      when: ({ valueOf }) => valueOf(schema.type) === 'TABLE',
    });
    required(schema.directory, { message: 'Directory is required' });
    required(schema.fileName, { message: 'File name is required' });
  });

  protected readonly canFilter = computed(() => this.mode === 'backup' && this.model().type !== 'TABLE');
  protected readonly isFullRecovery = computed(() => this.mode === 'recovery' && this.model().type === 'FULL');

  protected readonly loading = signal(false);
  protected readonly outcome = signal<Outcome | null>(null);
  protected readonly run = signal<DataPumpResult | null>(null);
  protected readonly logLines = signal<string[] | null>(null);
  protected readonly logError = signal('');

  /** Directory and log file of the last run, so "View log" shows the log of what was just executed. */
  private lastRun: { directory: string; logFile: string } | null = null;

  protected onSubmit(event: Event): void {
    event.preventDefault();
    submit(this.dataForm, async () => {
      const value = this.model();
      const request = this.buildRequest();
      this.loading.set(true);
      this.outcome.set(null);
      this.run.set(null);
      this.logLines.set(null);
      this.logError.set('');
      try {
        const response = await firstValueFrom(
          this.mode === 'backup'
            ? this.api.backup(request)
            : this.api.recover({ ...request, confirmFull: value.confirmFull }),
        );
        this.outcome.set({ ok: response.ok, message: response.message });
        this.run.set(response.data);
      } catch (error) {
        // A failed run still carries the command and the output, which explain what went wrong.
        this.outcome.set({ ok: false, message: error instanceof ApiError ? error.message : 'Unexpected error' });
        this.run.set(error instanceof ApiError ? ((error.data as DataPumpResult | null) ?? null) : null);
      } finally {
        this.lastRun = {
          directory: value.directory.trim(),
          logFile: this.mode === 'backup' ? `${value.fileName.trim()}.log` : `${value.fileName.trim()}_imp.log`,
        };
        this.loading.set(false);
      }
    });
  }

  protected async viewLog(): Promise<void> {
    if (!this.lastRun) {
      return;
    }
    this.logLines.set(null);
    this.logError.set('');
    try {
      const response = await firstValueFrom(this.api.readLog(this.lastRun.directory, this.lastRun.logFile));
      this.logLines.set(response.data ?? []);
    } catch (error) {
      this.logError.set(error instanceof ApiError ? error.message : 'Unexpected error');
    }
  }

  private buildRequest(): BackupRequest {
    const value = this.model();
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
