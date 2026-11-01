import { Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { BackupApi } from './backup-api';
import { Row } from './backup.models';

/** `2026-10-27T19:31:36.000+00:00`, `2026-10-27T19:31:36Z` or with another offset; the offset is required. */
const ISO_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$/;

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
    required(schema.schema, { message: 'El esquema es obligatorio' });
    required(schema.table, { message: 'La tabla es obligatoria' });
  });

  protected readonly rowsModel = signal({ schema: '', table: '', timestamp: '' });
  protected readonly rowsForm = form(this.rowsModel, (schema) => {
    required(schema.schema, { message: 'El esquema es obligatorio' });
    required(schema.table, { message: 'La tabla es obligatoria' });
    required(schema.timestamp, { message: 'La fecha y hora es obligatoria' });
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

  /**
   * The API serializes dates as ISO strings in UTC (`2026-10-27T19:31:36.000+00:00`). Show them in the browser's time
   * zone, which is where the person using the screen expects them; any other value is shown as it is.
   */
  protected cell(value: unknown): string {
    if (value === null || value === undefined) {
      return '';
    }
    if (typeof value === 'string' && ISO_DATE_TIME.test(value)) {
      const date = new Date(value);
      if (!Number.isNaN(date.getTime())) {
        const pad = (n: number) => String(n).padStart(2, '0');
        return (
          `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
          `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
        );
      }
    }
    return String(value);
  }

  private rowsRequest() {
    const { schema, table, timestamp } = this.rowsModel();
    // A datetime-local input omits the seconds when they are zero; the API expects a full ISO local date-time.
    const withSeconds = timestamp.length === 16 ? `${timestamp}:00` : timestamp;
    return { schema: schema.trim(), table: table.trim(), timestamp: withSeconds };
  }
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Error inesperado';
}
