import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-tuning',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Query Tuning &amp; Performance</h1>
    <nav class="subnav" aria-label="Query tuning">
      <a routerLink="/tuning/plan" routerLinkActive="active">Execution plan</a>
      <a routerLink="/tuning/statistics" routerLinkActive="active">Statistics &amp; indexes</a>
      <a routerLink="/tuning/monitoring" routerLinkActive="active">Monitoring</a>
    </nav>
    <router-outlet />
  `,
  styleUrl: './tuning.css',
})
export class Tuning {}
