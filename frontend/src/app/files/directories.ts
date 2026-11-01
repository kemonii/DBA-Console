import { Component, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import { FilesApi } from './files-api';
import { DirectoryObject } from './files.models';

/**
 * Directory objects screen: list, create and drop Oracle directory objects, and grant or
 * revoke READ and WRITE on them. Follows the pattern of `example/example.ts`.
 */
@Component({
  selector: 'app-directories',
  imports: [FormField, RouterLink],
  templateUrl: './directories.html',
  styleUrl: './directories.css',
})
export class Directories {
  private readonly filesApi = inject(FilesApi);

  protected readonly directories = signal<DirectoryObject[]>([]);
  protected readonly loaded = signal(false);
  protected readonly listError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  /** Name of the directory waiting for the drop confirmation, or '' when there is none. */
  protected readonly pendingDrop = signal('');

  protected readonly createModel = signal({ name: '', path: '' });
  protected readonly createForm = form(this.createModel, (schema) => {
    required(schema.name, { message: 'Ingrese un nombre de directorio' });
    required(schema.path, { message: 'Ingrese la ruta del directorio' });
  });

  protected readonly accessModel = signal({ directory: '', user: '' });
  protected readonly accessForm = form(this.accessModel, (schema) => {
    required(schema.directory, { message: 'Seleccione un directorio' });
    required(schema.user, { message: 'Ingrese un nombre de usuario' });
  });

  constructor() {
    void this.load();
  }

  protected onCreate(event: Event): void {
    event.preventDefault();
    submit(this.createForm, async () => {
      const { name, path } = this.createModel();
      const done = await this.run(() => this.filesApi.createDirectory(name, path));
      if (done) {
        this.createModel.set({ name: '', path: '' });
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
    if (name && (await this.run(() => this.filesApi.dropDirectory(name)))) {
      await this.load();
    }
  }

  protected onGrant(event: Event): void {
    event.preventDefault();
    submit(this.accessForm, async () => {
      const { directory, user } = this.accessModel();
      await this.run(() => this.filesApi.grant(directory, user));
    });
  }

  protected onRevoke(): void {
    submit(this.accessForm, async () => {
      const { directory, user } = this.accessModel();
      await this.run(() => this.filesApi.revoke(directory, user));
    });
  }

  private async load(): Promise<void> {
    this.listError.set('');
    try {
      const result = await firstValueFrom(this.filesApi.listDirectories());
      this.directories.set(result.data ?? []);
      this.loaded.set(true);
    } catch (error) {
      this.listError.set(messageOf(error));
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
