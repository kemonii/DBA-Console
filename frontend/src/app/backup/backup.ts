import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Backup & Recovery module page: a title, a menu with the module's screens (backup, recovery and flashback) and the
 * routed screen. The routes are in `backup.routes.ts`.
 */
@Component({
  selector: 'app-backup',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Respaldos y recuperación</h1>
    <nav class="tabs" aria-label="Pantallas de respaldos y recuperación">
      <a routerLink="." routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Respaldo</a>
      <a routerLink="recovery" routerLinkActive="active">Recuperación</a>
      <a routerLink="flashback" routerLinkActive="active">Flashback</a>
    </nav>
    <router-outlet />
  `,
  styles: `
    .tabs {
      display: flex;
      gap: 1rem;
      margin-bottom: 1.5rem;
      border-bottom: 1px solid #d1d5db;
    }

    .tabs a {
      padding: 0.5rem 0.25rem;
      color: #1f2937;
      text-decoration: none;
      border-bottom: 2px solid transparent;
    }

    .tabs a.active {
      font-weight: 600;
      border-bottom-color: #1f2937;
    }
  `,
})
export class Backup {}
