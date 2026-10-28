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
    <h1>Security</h1>
    <nav class="subnav" aria-label="Security">
      <a routerLink="/security/tablespaces" routerLinkActive="active">Tablespaces</a>
      <a routerLink="/security/users" routerLinkActive="active">Users</a>
      <a routerLink="/security/privileges" routerLinkActive="active">Roles &amp; privileges</a>
      <a routerLink="/security/profiles" routerLinkActive="active">Profiles</a>
      <a routerLink="/security/auditing" routerLinkActive="active">Auditing</a>
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
export class Security {}
