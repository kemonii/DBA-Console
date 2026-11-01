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
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 1.5rem;
      border-bottom: 1px solid #d1d5db;
    }

    .subnav a {
      padding: 0.5rem 0.25rem;
      color: #1f2937;
      text-decoration: none;
      border-bottom: 2px solid transparent;
    }

    .subnav a.active {
      font-weight: 600;
      border-bottom-color: #1f2937;
    }
  `,
})
export class Files {}
