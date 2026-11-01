import { Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { ActivatedRoute } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import { DbRole, GranteePrivileges } from './users.models';
import { UsersApi } from './users-api';

type GrantKind = 'system' | 'role' | 'object';

/** Object privileges offered as checkboxes; the backend accepts the full list from the course. */
export const OBJECT_PRIVILEGES = [
  'SELECT',
  'INSERT',
  'UPDATE',
  'DELETE',
  'EXECUTE',
  'ALTER',
  'REFERENCES',
  'INDEX',
];

const EMPTY_GRANT = {
  kind: 'system' as GrantKind,
  privilege: '',
  role: '',
  owner: '',
  object: '',
  withOption: false,
};

/**
 * Roles & privileges screen: create and drop roles, show what a user or role has been granted,
 * and grant or revoke system privileges, roles and object privileges. Opening it with
 * `?grantee=NAME` (the "Privileges" link of the users screen) shows that grantee directly.
 */
@Component({
  selector: 'app-privileges',
  imports: [FormField],
  templateUrl: './privileges.html',
  styleUrl: './security-screen.css',
})
export class Privileges {
  private readonly usersApi = inject(UsersApi);

  protected readonly objectPrivilegeNames = OBJECT_PRIVILEGES;

  protected readonly roles = signal<DbRole[]>([]);
  protected readonly customRoles = computed(() =>
    this.roles().filter((role) => !role.oracleMaintained),
  );
  protected readonly oracleRoleNames = computed(() =>
    this.roles()
      .filter((role) => role.oracleMaintained)
      .map((role) => role.name)
      .join(', '),
  );
  protected readonly systemPrivileges = signal<string[]>([]);
  private readonly userNames = signal<string[]>([]);
  /** User and role names offered as suggestions for the grantee field. */
  protected readonly granteeNames = computed(() => [
    'PUBLIC',
    ...this.userNames(),
    ...this.roles().map((role) => role.name),
  ]);
  protected readonly rolesError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly pendingRoleDrop = signal('');

  protected readonly privileges = signal<GranteePrivileges | null>(null);
  protected readonly privilegesError = signal('');

  protected readonly roleModel = signal({ name: '' });
  protected readonly roleForm = form(this.roleModel, (schema) => {
    required(schema.name, { message: 'Ingrese un nombre de rol' });
  });

  protected readonly granteeModel = signal({ grantee: '' });
  protected readonly granteeForm = form(this.granteeModel, (schema) => {
    required(schema.grantee, { message: 'Ingrese un usuario o rol' });
  });

  protected readonly grantModel = signal({ ...EMPTY_GRANT });
  protected readonly grantForm = form(this.grantModel, (schema) => {
    required(schema.privilege, {
      message: 'Seleccione un privilegio de sistema',
      when: ({ valueOf }) => valueOf(schema.kind) === 'system',
    });
    required(schema.role, {
      message: 'Seleccione un rol',
      when: ({ valueOf }) => valueOf(schema.kind) === 'role',
    });
    required(schema.owner, {
      message: 'Ingrese el propietario (esquema) del objeto',
      when: ({ valueOf }) => valueOf(schema.kind) === 'object',
    });
    required(schema.object, {
      message: 'Ingrese el nombre del objeto',
      when: ({ valueOf }) => valueOf(schema.kind) === 'object',
    });
  });
  protected readonly selectedObjectPrivileges = signal<string[]>(['SELECT']);

  constructor() {
    void this.loadRoles();
    void this.loadSystemPrivileges();
    void this.loadUserNames();
    const grantee = inject(ActivatedRoute).snapshot.queryParamMap.get('grantee');
    if (grantee) {
      this.granteeModel.set({ grantee });
      void this.loadPrivileges();
    }
  }

  // ---- roles -------------------------------------------------------------------------------------

  protected onCreateRole(event: Event): void {
    event.preventDefault();
    submit(this.roleForm, async () => {
      const { name } = this.roleModel();
      if (await this.run(() => this.usersApi.createRole(name))) {
        this.roleModel.set({ name: '' });
        await this.loadRoles();
      }
    });
  }

  protected askRoleDrop(name: string): void {
    this.pendingRoleDrop.set(name);
  }

  protected cancelRoleDrop(): void {
    this.pendingRoleDrop.set('');
  }

  protected async confirmRoleDrop(): Promise<void> {
    const name = this.pendingRoleDrop();
    this.pendingRoleDrop.set('');
    if (name && (await this.run(() => this.usersApi.dropRole(name)))) {
      await this.loadRoles();
    }
  }

  // ---- privileges of a grantee -------------------------------------------------------------------

  protected onShow(event: Event): void {
    event.preventDefault();
    submit(this.granteeForm, async () => {
      await this.loadPrivileges();
    });
  }

  protected isObjectPrivilegeSelected(privilege: string): boolean {
    return this.selectedObjectPrivileges().includes(privilege);
  }

  protected toggleObjectPrivilege(privilege: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedObjectPrivileges.update((selected) =>
      checked ? [...selected, privilege] : selected.filter((p) => p !== privilege),
    );
  }

  protected onGrant(event: Event): void {
    event.preventDefault();
    const grantee = this.privileges()?.grantee;
    if (!grantee) {
      return;
    }
    submit(this.grantForm, async () => {
      const value = this.grantModel();
      let action: () => Observable<OperationResult>;
      if (value.kind === 'system') {
        action = () =>
          this.usersApi.grantSystemPrivilege(value.privilege, grantee, value.withOption);
      } else if (value.kind === 'role') {
        action = () => this.usersApi.grantRole(value.role, grantee, value.withOption);
      } else {
        const selected = this.selectedObjectPrivileges();
        if (selected.length === 0) {
          this.errorMessage.set('Seleccione al menos un privilegio sobre el objeto');
          return;
        }
        action = () =>
          this.usersApi.grantObjectPrivileges(
            selected,
            value.owner,
            value.object,
            grantee,
            value.withOption,
          );
      }
      if (await this.run(action)) {
        await this.loadPrivileges();
      }
    });
  }

  protected async revokeSystemPrivilege(privilege: string): Promise<void> {
    await this.revoke((grantee) => this.usersApi.revokeSystemPrivilege(privilege, grantee));
  }

  protected async revokeRole(role: string): Promise<void> {
    await this.revoke((grantee) => this.usersApi.revokeRole(role, grantee));
  }

  protected async revokeObjectPrivilege(
    owner: string,
    object: string,
    privilege: string,
  ): Promise<void> {
    await this.revoke((grantee) =>
      this.usersApi.revokeObjectPrivileges([privilege], owner, object, grantee),
    );
  }

  private async revoke(action: (grantee: string) => Observable<OperationResult>): Promise<void> {
    const grantee = this.privileges()?.grantee;
    if (grantee && (await this.run(() => action(grantee)))) {
      await this.loadPrivileges();
    }
  }

  // ---- loading -----------------------------------------------------------------------------------

  private async loadPrivileges(): Promise<void> {
    this.privilegesError.set('');
    try {
      const result = await firstValueFrom(this.usersApi.privilegesOf(this.granteeModel().grantee));
      this.privileges.set(result.data);
    } catch (error) {
      this.privileges.set(null);
      this.privilegesError.set(messageOf(error));
    }
  }

  private async loadRoles(): Promise<void> {
    this.rolesError.set('');
    try {
      const result = await firstValueFrom(this.usersApi.listRoles());
      this.roles.set(result.data ?? []);
    } catch (error) {
      this.rolesError.set(messageOf(error));
    }
  }

  /** Only fills the privilege select; a failure leaves it empty. */
  private async loadSystemPrivileges(): Promise<void> {
    try {
      const result = await firstValueFrom(this.usersApi.listSystemPrivileges());
      this.systemPrivileges.set(result.data ?? []);
    } catch {
      this.systemPrivileges.set([]);
    }
  }

  /** Only fills the grantee suggestions; a failure leaves them with roles only. */
  private async loadUserNames(): Promise<void> {
    try {
      const result = await firstValueFrom(this.usersApi.listUsers());
      this.userNames.set((result.data ?? []).map((user) => user.username));
    } catch {
      this.userNames.set([]);
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
