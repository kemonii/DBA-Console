import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BackupApiService } from './backup-api.service';
import { OperationResult, Row } from './backup.models';

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
  imports: [ReactiveFormsModule],
  templateUrl: './flashback-page.html',
  styleUrl: './flashback-page.css',
})
export class FlashbackPage {
  private readonly api = inject(BackupApiService);

  protected readonly tableForm = new FormGroup({
    schema: new FormControl('', { nonNullable: true }),
    table: new FormControl('', { nonNullable: true }),
  });

  protected readonly rowsForm = new FormGroup({
    schema: new FormControl('', { nonNullable: true }),
    table: new FormControl('', { nonNullable: true }),
    timestamp: new FormControl('', { nonNullable: true }),
  });

  protected readonly tableOutcome = signal<Outcome | null>(null);
  protected readonly rowsOutcome = signal<Outcome | null>(null);
  protected readonly rows = signal<Row[] | null>(null);
  protected readonly columns = computed(() => Object.keys(this.rows()?.[0] ?? {}));

  protected restoreTable(): void {
    const { schema, table } = this.tableForm.getRawValue();
    if (!schema.trim() || !table.trim()) {
      this.tableOutcome.set({ ok: false, message: 'Schema and table are required' });
      return;
    }
    this.api.flashbackTable({ schema: schema.trim(), table: table.trim() }).subscribe((result) => {
      this.tableOutcome.set(toOutcome(result));
    });
  }

  protected preview(): void {
    const request = this.rowsRequest();
    if (!request) {
      return;
    }
    this.api.flashbackQuery(request).subscribe((result) => {
      if (result.ok) {
        this.rows.set(result.data ?? []);
        this.rowsOutcome.set(null);
      } else {
        this.rows.set(null);
        this.rowsOutcome.set(toOutcome(result));
      }
    });
  }

  protected restoreRows(): void {
    const request = this.rowsRequest();
    if (!request) {
      return;
    }
    this.api.flashbackRestore(request).subscribe((result) => {
      this.rowsOutcome.set(toOutcome(result));
    });
  }

  private rowsRequest() {
    const { schema, table, timestamp } = this.rowsForm.getRawValue();
    if (!schema.trim() || !table.trim() || !timestamp) {
      this.rowsOutcome.set({ ok: false, message: 'Schema, table and timestamp are required' });
      return null;
    }
    // A datetime-local input omits the seconds when they are zero; the API expects a full ISO local date-time.
    const withSeconds = timestamp.length === 16 ? `${timestamp}:00` : timestamp;
    return { schema: schema.trim(), table: table.trim(), timestamp: withSeconds };
  }
}

function toOutcome(result: OperationResult<unknown>): Outcome {
  return { ok: result.ok, message: result.message };
}
