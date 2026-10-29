import { Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TuningApi } from './tuning-api';
import { DataRow, MonitorView } from './tuning.models';
import { TuningDataTable } from './data-table';

@Component({
  selector: 'app-monitoring',
  imports: [TuningDataTable],
  templateUrl: './monitoring.html',
  styleUrl: './tuning.css',
})
export class Monitoring {
  private readonly api = inject(TuningApi);
  protected readonly views: { path: MonitorView; label: string }[] = [
    { path: 'instance', label: 'Instance status' },
    { path: 'parameters', label: 'Main parameters' },
    { path: 'sessions', label: 'Sessions and open cursors' },
    { path: 'sessions/programs', label: 'Sessions per program' },
    { path: 'sessions/users', label: 'Sessions per user' },
    { path: 'locks', label: 'Blocking locks' },
    { path: 'locks/objects', label: 'Locked objects' },
  ];
  protected readonly selected = signal<MonitorView>('instance');
  protected readonly shown = signal('');
  protected readonly rows = signal<DataRow[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');
  protected readonly pendingKill = signal<{ sid: number; serial: number } | null>(null);
  constructor() {
    void this.load();
  }
  protected select(event: Event): void {
    this.selected.set((event.target as HTMLSelectElement).value as MonitorView);
    void this.load();
  }
  protected async load(): Promise<void> {
    if (this.busy()) return;
    const view = this.selected();
    this.busy.set(true);
    this.error.set('');
    this.rows.set(null);
    this.pendingKill.set(null);
    try {
      const result = await firstValueFrom(this.api.monitor(view));
      this.shown.set(this.views.find((v) => v.path === view)?.label ?? view);
      this.rows.set(result.data ?? []);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unexpected error');
    } finally {
      this.busy.set(false);
    }
  }
  protected requestKill(row: DataRow): void {
    this.pendingKill.set({
      sid: Number(row['BLOCKER_SID']),
      serial: Number(row['BLOCKER_SERIAL']),
    });
  }
  protected async kill(): Promise<void> {
    const target = this.pendingKill();
    if (!target || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    try {
      this.message.set((await firstValueFrom(this.api.kill(target.sid, target.serial))).message);
      this.busy.set(false);
      await this.load();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unexpected error');
    } finally {
      this.busy.set(false);
    }
  }
}
