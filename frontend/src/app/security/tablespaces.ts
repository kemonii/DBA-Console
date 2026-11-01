import { Component, inject, signal } from '@angular/core';
import { FormField, form, pattern, required, submit } from '@angular/forms/signals';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';
import { TablespacesApi } from './tablespaces-api';
import { Datafile, TablespaceUsage } from './tablespaces.models';

/** A positive whole number of megabytes. */
const MEGABYTES = /^[1-9]\d*$/;
const MEGABYTES_OR_EMPTY = /^([1-9]\d*)?$/;

/**
 * Tablespaces screen: list tablespaces with their space usage, create and drop them, and add or
 * resize the datafiles of the selected one. Follows the pattern of `files/directories.ts`.
 */
@Component({
  selector: 'app-tablespaces',
  imports: [FormField],
  templateUrl: './tablespaces.html',
  styleUrl: './tablespaces.css',
})
export class Tablespaces {
  private readonly tablespacesApi = inject(TablespacesApi);

  protected readonly tablespaces = signal<TablespaceUsage[]>([]);
  protected readonly loaded = signal(false);
  protected readonly listError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  /** Name of the tablespace waiting for the drop confirmation, or '' when there is none. */
  protected readonly pendingDrop = signal('');

  /** Tablespace whose datafiles are shown, or '' when none is selected. */
  protected readonly selected = signal('');
  protected readonly datafiles = signal<Datafile[]>([]);
  protected readonly datafilesError = signal('');

  protected readonly createModel = signal({
    name: '',
    fileName: '',
    sizeMb: '100',
    nextMb: '10',
    maxSizeMb: '',
  });
  protected readonly createForm = form(this.createModel, (schema) => {
    required(schema.name, { message: 'Ingrese un nombre de tablespace' });
    required(schema.fileName, {
      message: 'Ingrese un nombre de datafile, por ejemplo ventas01.dbf',
    });
    required(schema.sizeMb, { message: 'Ingrese el tamaño inicial' });
    pattern(schema.sizeMb, MEGABYTES, { message: 'Use un número entero de MB' });
    required(schema.nextMb, { message: 'Ingrese el incremento de autoextensión' });
    pattern(schema.nextMb, MEGABYTES, { message: 'Use un número entero de MB' });
    pattern(schema.maxSizeMb, MEGABYTES_OR_EMPTY, { message: 'Use un número entero de MB' });
  });

  protected readonly addModel = signal({ fileName: '', sizeMb: '100' });
  protected readonly addForm = form(this.addModel, (schema) => {
    required(schema.fileName, { message: 'Ingrese un nombre de datafile' });
    required(schema.sizeMb, { message: 'Ingrese el tamaño' });
    pattern(schema.sizeMb, MEGABYTES, { message: 'Use un número entero de MB' });
  });

  protected readonly resizeModel = signal({ fileId: '', sizeMb: '' });
  protected readonly resizeForm = form(this.resizeModel, (schema) => {
    required(schema.fileId, { message: 'Seleccione un datafile' });
    required(schema.sizeMb, { message: 'Ingrese el nuevo tamaño' });
    pattern(schema.sizeMb, MEGABYTES, { message: 'Use un número entero de MB' });
  });

  constructor() {
    void this.load();
  }

  protected onCreate(event: Event): void {
    event.preventDefault();
    submit(this.createForm, async () => {
      const { name, fileName, sizeMb, nextMb, maxSizeMb } = this.createModel();
      const request = {
        name,
        fileName,
        sizeMb: Number(sizeMb),
        nextMb: Number(nextMb),
        ...(maxSizeMb ? { maxSizeMb: Number(maxSizeMb) } : {}),
      };
      if (await this.run(() => this.tablespacesApi.create(request))) {
        this.createModel.set({
          name: '',
          fileName: '',
          sizeMb: '100',
          nextMb: '10',
          maxSizeMb: '',
        });
        await this.load();
      }
    });
  }

  protected async select(name: string): Promise<void> {
    this.selected.set(name);
    this.resizeModel.set({ fileId: '', sizeMb: '' });
    await this.loadDatafiles();
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
    if (name && (await this.run(() => this.tablespacesApi.drop(name)))) {
      if (this.selected() === name) {
        this.selected.set('');
      }
      await this.load();
    }
  }

  protected onAddDatafile(event: Event): void {
    event.preventDefault();
    submit(this.addForm, async () => {
      const { fileName, sizeMb } = this.addModel();
      const tablespace = this.selected();
      if (
        await this.run(() => this.tablespacesApi.addDatafile(tablespace, fileName, Number(sizeMb)))
      ) {
        this.addModel.set({ fileName: '', sizeMb: '100' });
        await this.refreshSelected();
      }
    });
  }

  protected onResize(event: Event): void {
    event.preventDefault();
    submit(this.resizeForm, async () => {
      const { fileId, sizeMb } = this.resizeModel();
      const tablespace = this.selected();
      const resize = () =>
        this.tablespacesApi.resizeDatafile(tablespace, Number(fileId), Number(sizeMb));
      if (await this.run(resize)) {
        await this.refreshSelected();
      }
    });
  }

  private async load(): Promise<void> {
    this.listError.set('');
    try {
      const result = await firstValueFrom(this.tablespacesApi.list());
      this.tablespaces.set(result.data ?? []);
      this.loaded.set(true);
    } catch (error) {
      this.listError.set(messageOf(error));
    }
  }

  private async loadDatafiles(): Promise<void> {
    this.datafilesError.set('');
    this.datafiles.set([]);
    try {
      const result = await firstValueFrom(this.tablespacesApi.listDatafiles(this.selected()));
      this.datafiles.set(result.data ?? []);
    } catch (error) {
      this.datafilesError.set(messageOf(error));
    }
  }

  /** Sizes change after adding or resizing a datafile, so both lists are read again. */
  private async refreshSelected(): Promise<void> {
    await Promise.all([this.load(), this.loadDatafiles()]);
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
