import { Component, inject, signal } from '@angular/core';
import { FormField, form, pattern, required, submit } from '@angular/forms/signals';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import {
  AuditOptions,
  AuditTrailParameter,
  ObjectEntry,
  SessionEntry,
  TrailEntry,
  Whenever,
} from './auditing.models';
import { AuditingApi } from './auditing-api';

type TrailView = 'sessions' | 'objects' | 'all';

/** Table actions audited in class. */
export const TABLE_ACTIONS = ['INSERT', 'UPDATE', 'DELETE', 'SELECT'];

const NOT_AUDITED = '-/-';

/**
 * Auditing screen: show the `audit_trail` parameter and what is audited, turn session and table
 * auditing on and off, and read the audit trail. Follows the pattern of `tablespaces.ts`.
 */
@Component({
  selector: 'app-auditing',
  imports: [FormField],
  templateUrl: './auditing.html',
  styleUrl: './security-screen.css',
})
export class Auditing {
  private readonly auditingApi = inject(AuditingApi);

  protected readonly tableActions = TABLE_ACTIONS;

  protected readonly parameter = signal<AuditTrailParameter | null>(null);
  protected readonly options = signal<AuditOptions | null>(null);
  protected readonly loadError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  protected readonly sessionWhenever = signal<Whenever>('ALWAYS');

  protected readonly tableModel = signal({ owner: '', table: '', byAccess: true });
  protected readonly tableForm = form(this.tableModel, (schema) => {
    required(schema.owner, { message: 'Ingrese el propietario (esquema) de la tabla' });
    required(schema.table, { message: 'Ingrese el nombre de la tabla' });
  });
  protected readonly selectedActions = signal<string[]>(['INSERT', 'UPDATE', 'DELETE']);

  protected readonly trailModel = signal({
    view: 'sessions' as TrailView,
    username: '',
    limit: '100',
  });
  protected readonly trailForm = form(this.trailModel, (schema) => {
    pattern(schema.limit, /^([1-9]\d{0,2}|1000)$/, { message: 'Use un número del 1 al 1000' });
  });
  /** View of the rows below, which stays as it was loaded even if the selector changes. */
  protected readonly shownView = signal<TrailView | null>(null);
  protected readonly sessionRows = signal<SessionEntry[]>([]);
  protected readonly objectRows = signal<ObjectEntry[]>([]);
  protected readonly trailRows = signal<TrailEntry[]>([]);
  protected readonly trailError = signal('');

  constructor() {
    void this.loadSettings();
  }

  protected audited(value: string): boolean {
    return value !== NOT_AUDITED;
  }

  protected setSessionWhenever(event: Event): void {
    this.sessionWhenever.set((event.target as HTMLSelectElement).value as Whenever);
  }

  protected async auditSessions(enable: boolean): Promise<void> {
    const whenever = this.sessionWhenever();
    const action = () =>
      enable
        ? this.auditingApi.auditSessions(whenever)
        : this.auditingApi.stopAuditingSessions(whenever);
    if (await this.run(action)) {
      await this.loadSettings();
    }
  }

  protected isActionSelected(action: string): boolean {
    return this.selectedActions().includes(action);
  }

  protected toggleAction(action: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedActions.update((selected) =>
      checked ? [...selected, action] : selected.filter((a) => a !== action),
    );
  }

  protected onAuditTable(event: Event): void {
    event.preventDefault();
    submit(this.tableForm, async () => {
      const { owner, table, byAccess } = this.tableModel();
      const actions = this.selectedActions();
      if (actions.length === 0) {
        this.errorMessage.set('Seleccione al menos una acción para auditar');
        return;
      }
      if (await this.run(() => this.auditingApi.auditTable(owner, table, actions, byAccess))) {
        await this.loadSettings();
      }
    });
  }

  /** NOAUDIT for every action that is audited on the table. */
  protected async stopAuditingTable(option: AuditOptions['objectOptions'][number]): Promise<void> {
    const values: Record<string, string> = {
      INSERT: option.insert,
      UPDATE: option.update,
      DELETE: option.delete,
      SELECT: option.select,
    };
    const actions = TABLE_ACTIONS.filter((action) => this.audited(values[action]));
    const stop = () => this.auditingApi.stopAuditingTable(option.owner, option.objectName, actions);
    if (await this.run(stop)) {
      await this.loadSettings();
    }
  }

  protected onReadTrail(event: Event): void {
    event.preventDefault();
    submit(this.trailForm, async () => {
      const { view, username, limit } = this.trailModel();
      const rows = Number(limit) || 100;
      this.trailError.set('');
      try {
        if (view === 'sessions') {
          this.sessionRows.set(
            (await firstValueFrom(this.auditingApi.sessions(username, rows))).data ?? [],
          );
        } else if (view === 'objects') {
          this.objectRows.set(
            (await firstValueFrom(this.auditingApi.objects(username, rows))).data ?? [],
          );
        } else {
          this.trailRows.set(
            (await firstValueFrom(this.auditingApi.trail(username, rows))).data ?? [],
          );
        }
        this.shownView.set(view);
      } catch (error) {
        this.shownView.set(null);
        this.trailError.set(messageOf(error));
      }
    });
  }

  private async loadSettings(): Promise<void> {
    this.loadError.set('');
    try {
      const [parameter, options] = await Promise.all([
        firstValueFrom(this.auditingApi.parameter()),
        firstValueFrom(this.auditingApi.options()),
      ]);
      this.parameter.set(parameter.data);
      this.options.set(options.data);
    } catch (error) {
      this.loadError.set(messageOf(error));
    }
  }

  /** Runs one backend action and reports its outcome. Returns true when it succeeded. */
  private async run(action: () => Observable<OperationResult>): Promise<boolean> {
    this.busy.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    try {
      const result = await firstValueFrom(action());
      this.successMessage.set(result.message);
      return true;
    } catch (error) {
      this.errorMessage.set(messageOf(error));
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Error inesperado';
}
