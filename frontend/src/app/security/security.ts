import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Security shell: a heading, a sub-navigation and an outlet for the module's screens
 * (`tablespaces.ts` for now). Routes are defined in `security.routes.ts`.
 */
@Component({
  selector: 'app-security',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Security</h1>
    <nav class="subnav" aria-label="Security">
      <a routerLink="/security/tablespaces" routerLinkActive="active">Tablespaces</a>
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
