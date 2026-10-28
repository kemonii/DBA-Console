import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../core/api-error';
import { FilesApi } from './files-api';
import { BackupFile, DirectoryObject, LogContent } from './files.models';
import { formatSize } from './format';

/**
 * Backup files screen: pick a directory object, list its `.dmp` and `.log` files, read a
 * log and delete a file after an explicit confirmation.
 *
 * The selected directory lives in the URL (`/files/directories/:name/files`), so a listing
 * can be bookmarked and the browser back button works.
 */
@Component({
  selector: 'app-backup-files',
  imports: [DatePipe],
  templateUrl: './backup-files.html',
  styleUrl: './backup-files.css',
})
export class BackupFiles {
  private readonly filesApi = inject(FilesApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly formatSize = formatSize;

  protected readonly directories = signal<DirectoryObject[]>([]);
  /** Name of the selected directory object, or '' when none is selected. */
  protected readonly directory = signal('');

  protected readonly files = signal<BackupFile[]>([]);
  protected readonly filesLoaded = signal(false);
  protected readonly listError = signal('');

  protected readonly busy = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  /** Name of the file waiting for the delete confirmation, or '' when there is none. */
  protected readonly pendingDelete = signal('');
  protected readonly log = signal<LogContent | null>(null);

  constructor() {
    void this.loadDirectories();
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.directory.set(params.get('name') ?? '');
      this.resetMessages();
      void this.loadFiles();
    });
  }

  protected isLog(fileName: string): boolean {
    return fileName.toLowerCase().endsWith('.log');
  }

  protected onSelectDirectory(event: Event): void {
    const name = (event.target as HTMLSelectElement).value;
    void (name
      ? this.router.navigate(['/files/directories', name, 'files'])
      : this.router.navigate(['/files/browse']));
  }

  protected async viewLog(fileName: string): Promise<void> {
    this.errorMessage.set('');
    this.successMessage.set('');
    try {
      const result = await firstValueFrom(this.filesApi.readLog(this.directory(), fileName));
      this.log.set(result.data);
    } catch (error) {
      this.errorMessage.set(messageOf(error));
    }
  }

  protected closeLog(): void {
    this.log.set(null);
  }

  protected askDelete(fileName: string): void {
    this.pendingDelete.set(fileName);
  }

  protected cancelDelete(): void {
    this.pendingDelete.set('');
  }

  protected async confirmDelete(): Promise<void> {
    const fileName = this.pendingDelete();
    this.pendingDelete.set('');
    if (!fileName) {
      return;
    }
    this.busy.set(true);
    this.resetMessages();
    try {
      const result = await firstValueFrom(this.filesApi.deleteFile(this.directory(), fileName));
      this.successMessage.set(result.message);
      if (this.log()?.name === fileName) {
        this.log.set(null);
      }
      await this.loadFiles();
    } catch (error) {
      this.errorMessage.set(messageOf(error));
    } finally {
      this.busy.set(false);
    }
  }

  private async loadDirectories(): Promise<void> {
    try {
      const result = await firstValueFrom(this.filesApi.listDirectories());
      this.directories.set(result.data ?? []);
    } catch (error) {
      this.listError.set(messageOf(error));
    }
  }

  private async loadFiles(): Promise<void> {
    const name = this.directory();
    this.files.set([]);
    this.filesLoaded.set(false);
    this.listError.set('');
    this.log.set(null);
    if (!name) {
      return;
    }
    try {
      const result = await firstValueFrom(this.filesApi.listFiles(name));
      // Ignore a late response for a directory that is no longer selected.
      if (name === this.directory()) {
        this.files.set(result.data ?? []);
        this.filesLoaded.set(true);
      }
    } catch (error) {
      if (name === this.directory()) {
        this.listError.set(messageOf(error));
      }
    }
  }

  private resetMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }
}

function messageOf(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Unexpected error';
}
