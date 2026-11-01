import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * File Management shell: a heading, a sub-navigation and an outlet for the module's screens
 * (`directories.ts` and `backup-files.ts`). Routes are defined in `files.routes.ts`.
 */
@Component({
  selector: 'app-files',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Administración de archivos</h1>
    <nav class="subnav" aria-label="Administración de archivos">
      <a routerLink="/files/directories" routerLinkActive="active">Directorios</a>
      <a routerLink="/files/browse" routerLinkActive="active">Archivos de respaldo</a>
    </nav>
    <router-outlet />
  `,
  styles: `
    .subnav {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .subnav a.active {
      font-weight: 600;
      text-decoration: underline;
    }
  `,
})
export class Files {}
