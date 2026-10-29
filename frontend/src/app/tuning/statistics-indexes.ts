import { Component, inject, signal } from '@angular/core';
import { form, FormField, required, submit } from '@angular/forms/signals';
import { Observable, firstValueFrom } from 'rxjs';
import { OperationResult } from '../core/operation-result.model';
import { TuningApi } from './tuning-api';
import { DataRow, IndexType } from './tuning.models';
import { TuningDataTable } from './data-table';

@Component({
  selector: 'app-statistics-indexes',
  imports: [FormField, TuningDataTable],
  templateUrl: './statistics-indexes.html',
  styleUrl: './tuning.css',
})
export class StatisticsIndexes {
  private readonly api = inject(TuningApi);
  protected readonly model = signal({ schema: '', table: '' });
  protected readonly fields = form(this.model, (s) => required(s.schema));
  protected readonly indexModel = signal({
    schema: '',
    table: '',
    name: '',
    columns: '',
    type: 'BTREE' as IndexType,
  });
  protected readonly indexFields = form(this.indexModel, (s) => {
    required(s.schema);
    required(s.table);
    required(s.name);
    required(s.columns);
  });
  protected readonly statistics = signal<DataRow[] | null>(null);
  protected readonly indexes = signal<DataRow[] | null>(null);
  protected readonly validation = signal<DataRow[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly shownSchema = signal('');
  protected readonly pendingDrop = signal<{ schema: string; name: string } | null>(null);

  protected load(event?: Event): void {
    event?.preventDefault();
    if (this.busy()) return;
    void submit(this.fields, async () => {
      await this.refresh(this.model().schema);
    });
  }
  protected gather(): void {
    if (this.busy()) return;
    void submit(this.fields, async () => {
      const { schema, table } = this.model();
      if (await this.run(() => this.api.gather(schema, table))) await this.refresh(schema);
    });
  }
  protected create(event: Event): void {
    event.preventDefault();
    if (this.busy()) return;
    void submit(this.indexFields, async () => {
      const { schema, table, name, columns, type } = this.indexModel();
      if (
        await this.run(() =>
          this.api.createIndex(
            schema,
            table,
            name,
            columns.split(',').map((c) => c.trim()),
            type,
          ),
        )
      ) {
        await this.refresh(schema);
      }
    });
  }
  protected async rebuild(row: DataRow): Promise<void> {
    if (this.busy()) return;
    const schema = String(row['OWNER']);
    if (await this.run(() => this.api.rebuild(schema, String(row['INDEX_NAME']))))
      await this.refresh(schema);
  }
  protected async validate(row: DataRow): Promise<void> {
    if (this.busy()) return;
    this.validation.set(null);
    await this.run(
      () => this.api.validate(String(row['OWNER']), String(row['INDEX_NAME'])),
      (result) => {
        this.validation.set((result.data as DataRow[] | null) ?? []);
      },
    );
  }
  protected requestDrop(row: DataRow): void {
    this.pendingDrop.set({ schema: String(row['OWNER']), name: String(row['INDEX_NAME']) });
  }
  protected async confirmDrop(): Promise<void> {
    const target = this.pendingDrop();
    if (!target || this.busy()) return;
    if (await this.run(() => this.api.drop(target.schema, target.name))) {
      this.pendingDrop.set(null);
      await this.refresh(target.schema);
    }
  }
  private async refresh(schema: string): Promise<void> {
    this.busy.set(true);
    this.error.set('');
    this.statistics.set(null);
    this.indexes.set(null);
    this.validation.set(null);
    this.pendingDrop.set(null);
    try {
      const [stats, indexes] = await Promise.all([
        firstValueFrom(this.api.statistics(schema)),
        firstValueFrom(this.api.indexes(schema)),
      ]);
      this.shownSchema.set(schema.toUpperCase());
      this.statistics.set(stats.data ?? []);
      this.indexes.set(indexes.data ?? []);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Error inesperado');
    } finally {
      this.busy.set(false);
    }
  }
  private async run(
    action: () => Observable<OperationResult>,
    accept?: (result: OperationResult) => void,
  ): Promise<boolean> {
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    try {
      const result = await firstValueFrom(action());
      this.message.set(result.message);
      accept?.(result);
      return true;
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Error inesperado');
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}
