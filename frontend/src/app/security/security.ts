import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Security shell: a heading, a sub-navigation and an outlet for the module's screens
 * (`tablespaces.ts`, `users.ts`, `privileges.ts`, `profiles.ts`
 * and `auditing.ts`). Routes are defined in `security.routes.ts`.
 */
@Component({
  selector: 'app-security',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Seguridad</h1>
    <nav class="subnav" aria-label="Seguridad">
      <a routerLink="/security/tablespaces" routerLinkActive="active">Tablespaces</a>
      <a routerLink="/security/users" routerLinkActive="active">Usuarios</a>
      <a routerLink="/security/privileges" routerLinkActive="active">Roles y privilegios</a>
      <a routerLink="/security/profiles" routerLinkActive="active">Perfiles</a>
      <a routerLink="/security/auditing" routerLinkActive="active">Auditoría</a>
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
export class Security {}
