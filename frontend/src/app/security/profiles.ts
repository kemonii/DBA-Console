import { Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import { DbProfile, PROFILE_LIMITS } from './profiles.models';
import { ProfilesApi } from './profiles-api';
import { UsersApi } from './users-api';

/** A whole number, UNLIMITED or DEFAULT, in any case. */
const LIMIT_VALUE = /^([1-9]\d{0,8}|UNLIMITED|DEFAULT)$/i;

/**
 * Profiles screen: compare the limits of every profile, create a profile or change the limits of
 * an existing one (also DEFAULT, as in class), drop it, and assign a profile to a user. Follows
 * the pattern of `tablespaces.ts`.
 */
@Component({
  selector: 'app-profiles',
  imports: [FormField],
  templateUrl: './profiles.html',
  styleUrl: './security-screen.css',
})
export class Profiles {
  private readonly profilesApi = inject(ProfilesApi);
  private readonly usersApi = inject(UsersApi);

  protected readonly limitFields = PROFILE_LIMITS;

  protected readonly profiles = signal<DbProfile[]>([]);
  protected readonly loaded = signal(false);
  protected readonly listError = signal('');
  protected readonly usernames = signal<string[]>([]);

  /** Every resource that appears in some profile, kernel limits first, for the comparison table. */
  protected readonly resources = computed(() => {
    const seen = new Map<string, string>();
    for (const profile of this.profiles()) {
      for (const limit of profile.limits) {
        seen.set(limit.resource, limit.resourceType);
      }
    }
    return [...seen.entries()]
      .sort(([a, typeA], [b, typeB]) => typeA.localeCompare(typeB) || a.localeCompare(b))
      .map(([resource]) => resource);
  });

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly pendingDrop = signal('');

  /** `target` is '' to create a new profile, or the name of the profile to change. */
  protected readonly editModel = signal({ target: '', name: '' });
  protected readonly editForm = form(this.editModel, (schema) => {
    required(schema.name, {
      message: 'Ingrese un nombre de perfil',
      when: ({ valueOf }) => valueOf(schema.target) === '',
    });
  });
  protected readonly limitValues = signal<Record<string, string>>({});

  protected readonly assignModel = signal({ username: '', profile: '' });
  protected readonly assignForm = form(this.assignModel, (schema) => {
    required(schema.username, { message: 'Seleccione un usuario' });
    required(schema.profile, { message: 'Seleccione un perfil' });
  });

  constructor() {
    void this.load();
    void this.loadUsernames();
  }

  protected limitOf(profile: DbProfile, resource: string): string {
    return profile.limits.find((limit) => limit.resource === resource)?.limit ?? '';
  }

  protected setLimit(resource: string, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.limitValues.update((values) => ({ ...values, [resource]: value }));
  }

  protected onSave(event: Event): void {
    event.preventDefault();
    submit(this.editForm, async () => {
      const limits: Record<string, string> = {};
      for (const [resource, raw] of Object.entries(this.limitValues())) {
        const value = raw.trim();
        if (!value) {
          continue;
        }
        if (!LIMIT_VALUE.test(value)) {
          this.errorMessage.set(`${resource} debe ser un número entero, UNLIMITED o DEFAULT`);
          return;
        }
        limits[resource] = value.toUpperCase();
      }
      if (Object.keys(limits).length === 0) {
        this.errorMessage.set('Establezca al menos un límite');
        return;
      }
      const { target, name } = this.editModel();
      const save = () =>
        target ? this.profilesApi.alter(target, limits) : this.profilesApi.create(name, limits);
      if (await this.run(save)) {
        this.editModel.set({ target: '', name: '' });
        this.limitValues.set({});
        await this.load();
      }
    });
  }

  protected askDrop(name: string): void {
    this.pendingDrop.set(name);
  }

  protected cancelDrop(): void {
    this.pendingDrop.set('');
  }

  protected async confirmDrop(): Promise<void> {
    const name = this.pendingDrop();
    this.pendingDrop.set('');
    if (name && (await this.run(() => this.profilesApi.drop(name)))) {
      await this.load();
    }
  }

  protected onAssign(event: Event): void {
    event.preventDefault();
    submit(this.assignForm, async () => {
      const { username, profile } = this.assignModel();
      await this.run(() => this.profilesApi.assign(username, profile));
    });
  }

  private async load(): Promise<void> {
    this.listError.set('');
    try {
      const result = await firstValueFrom(this.profilesApi.list());
      this.profiles.set(result.data ?? []);
      this.loaded.set(true);
    } catch (error) {
      this.listError.set(messageOf(error));
    }
  }

  /** Only fills the user select; a failure leaves it empty. */
  private async loadUsernames(): Promise<void> {
    try {
      const result = await firstValueFrom(this.usersApi.listUsers());
      this.usernames.set((result.data ?? []).map((user) => user.username));
    } catch {
      this.usernames.set([]);
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
