import { Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { BackupApi } from './backup-api';
import { Row } from './backup.models';

interface Outcome {
  ok: boolean;
  message: string;
}

/**
 * Flashback screens: bring back a dropped table from the recycle bin, and look at or restore the rows a table had at
 * an earlier moment.
 */
@Component({
  selector: 'app-flashback-page',
  imports: [FormField],
  templateUrl: './flashback-page.html',
  styleUrl: './flashback-page.css',
})
export class FlashbackPage {
  private readonly api = inject(BackupApi);

  protected readonly tableModel = signal({ schema: '', table: '' });
  protected readonly tableForm = form(this.tableModel, (schema) => {
    required(schema.schema, { message: 'Schema is required' });
    required(schema.table, { message: 'Table is required' });
  });

  protected readonly rowsModel = signal({ schema: '', table: '', timestamp: '' });
  protected readonly rowsForm = form(this.rowsModel, (schema) => {
    required(schema.schema, { message: 'Schema is required' });
    required(schema.table, { message: 'Table is required' });
    required(schema.timestamp, { message: 'Timestamp is required' });
  });

  protected readonly tableOutcome = signal<Outcome | null>(null);
  protected readonly rowsOutcome = signal<Outcome | null>(null);
  protected readonly rows = signal<Row[] | null>(null);
  protected readonly columns = computed(() => Object.keys(this.rows()?.[0] ?? {}));
  protected readonly busy = signal(false);

  protected restoreTable(event: Event): void {
    event.preventDefault();
    submit(this.tableForm, async () => {
      const { schema, table } = this.tableModel();
      this.busy.set(true);
      try {
        const response = await firstValueFrom(
          this.api.flashbackTable({ schema: schema.trim(), table: table.trim() }),
        );
        this.tableOutcome.set({ ok: response.ok, message: response.message });
      } catch (error) {
        this.tableOutcome.set({ ok: false, message: messageOf(error) });
      } finally {
        this.busy.set(false);
      }
    });
  }

  protected preview(event?: Event): void {
    event?.preventDefault();
    submit(this.rowsForm, async () => {
      this.busy.set(true);
      try {
        const response = await firstValueFrom(this.api.flashbackQuery(this.rowsRequest()));
        this.rows.set(response.data ?? []);
        this.rowsOutcome.set(null);
      } catch (error) {
        this.rows.set(null);
        this.rowsOutcome.set({ ok: false, message: messageOf(error) });
      } finally {
        this.busy.set(false);
      }
    });
  }

  protected restoreRows(): void {
    submit(this.rowsForm, async () => {
      this.busy.set(true);
      try {
        const response = await firstValueFrom(this.api.flashbackRestore(this.rowsRequest()));
        this.rowsOutcome.set({ ok: response.ok, message: response.message });
      } catch (error) {
        this.rowsOutcome.set({ ok: false, message: messageOf(error) });
      } finally {
        this.busy.set(false);
      }
    });
  }

  private rowsRequest() {
    const { schema, table, timestamp } = this.rowsModel();
    // A datetime-local input omits the seconds when they are zero; the API expects a full ISO local date-time.
    const withSeconds = timestamp.length === 16 ? `${timestamp}:00` : timestamp;
    return { schema: schema.trim(), table: table.trim(), timestamp: withSeconds };
  }
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Unexpected error';
}
