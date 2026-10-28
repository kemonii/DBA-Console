import { Component, inject, signal } from '@angular/core';
import { FormField, form, pattern, required, submit } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import { TablespacesApi } from './tablespaces-api';
import { CreateUserRequest, DbUser } from './users.models';
import { UsersApi } from './users-api';

type QuotaMode = 'none' | 'limited' | 'unlimited';

const EMPTY_USER = {
  username: '',
  password: '',
  defaultTablespace: '',
  temporaryTablespace: '',
  quotaMode: 'none' as QuotaMode,
  quotaMb: '',
};

/**
 * Users screen: list the users created by administrators, create them, lock or unlock them, and
 * drop them. The privileges of a user are managed in `privileges.ts`. Follows the pattern of
 * `tablespaces.ts`.
 */
@Component({
  selector: 'app-users',
  imports: [FormField, RouterLink],
  templateUrl: './users.html',
  styleUrl: './security-screen.css',
})
export class Users {
  private readonly usersApi = inject(UsersApi);
  private readonly tablespacesApi = inject(TablespacesApi);

  protected readonly users = signal<DbUser[]>([]);
  protected readonly tablespaces = signal<string[]>([]);
  protected readonly loaded = signal(false);
  protected readonly listError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  /** User waiting for the drop confirmation, or '' when there is none. */
  protected readonly pendingDrop = signal('');
  protected readonly dropCascade = signal(false);

  protected readonly createModel = signal({ ...EMPTY_USER });
  protected readonly createForm = form(this.createModel, (schema) => {
    required(schema.username, { message: 'Enter a user name' });
    required(schema.password, { message: 'Enter a password' });
    pattern(schema.password, /^[^"]*$/, { message: 'The password cannot contain double quotes' });
    required(schema.defaultTablespace, {
      message: 'A quota needs a default tablespace',
      when: ({ valueOf }) => valueOf(schema.quotaMode) !== 'none',
    });
    required(schema.quotaMb, {
      message: 'Enter the quota',
      when: ({ valueOf }) => valueOf(schema.quotaMode) === 'limited',
    });
    pattern(schema.quotaMb, /^([1-9]\d*)?$/, { message: 'Use a whole number of MB' });
  });

  constructor() {
    void this.load();
    void this.loadTablespaces();
  }

  protected isLocked(user: DbUser): boolean {
    return user.accountStatus.includes('LOCKED');
  }

  protected onCreate(event: Event): void {
    event.preventDefault();
    submit(this.createForm, async () => {
      const value = this.createModel();
      const request: CreateUserRequest = { username: value.username, password: value.password };
      if (value.defaultTablespace) {
        request.defaultTablespace = value.defaultTablespace;
      }
      if (value.temporaryTablespace) {
        request.temporaryTablespace = value.temporaryTablespace;
      }
      if (value.quotaMode === 'limited') {
        request.quotaMb = Number(value.quotaMb);
      } else if (value.quotaMode === 'unlimited') {
        request.unlimitedQuota = true;
      }
      if (await this.run(() => this.usersApi.createUser(request))) {
        this.createModel.set({ ...EMPTY_USER });
        await this.load();
      }
    });
  }

  protected async toggleLock(user: DbUser): Promise<void> {
    const locked = !this.isLocked(user);
    if (await this.run(() => this.usersApi.setLocked(user.username, locked))) {
      await this.load();
    }
  }

  protected askDrop(username: string): void {
    this.dropCascade.set(false);
    this.pendingDrop.set(username);
  }

  protected cancelDrop(): void {
    this.pendingDrop.set('');
  }

  protected setCascade(event: Event): void {
    this.dropCascade.set((event.target as HTMLInputElement).checked);
  }

  protected async confirmDrop(): Promise<void> {
    const username = this.pendingDrop();
    const cascade = this.dropCascade();
    this.pendingDrop.set('');
    if (username && (await this.run(() => this.usersApi.dropUser(username, cascade)))) {
      await this.load();
    }
  }

  private async load(): Promise<void> {
    this.listError.set('');
    try {
      const result = await firstValueFrom(this.usersApi.listUsers());
      this.users.set(result.data ?? []);
      this.loaded.set(true);
    } catch (error) {
      this.listError.set(messageOf(error));
    }
  }

  /** The tablespace list only fills the select; without it the user can still be created. */
  private async loadTablespaces(): Promise<void> {
    try {
      const result = await firstValueFrom(this.tablespacesApi.list());
      this.tablespaces.set((result.data ?? []).map((tablespace) => tablespace.name));
    } catch {
      this.tablespaces.set([]);
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
  return error instanceof ApiError ? error.message : 'Unexpected error';
}
