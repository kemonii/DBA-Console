import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-tuning',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <h1>Tunning de consultas y performance</h1>
    <nav class="subnav" aria-label="Tunning de consultas">
      <a routerLink="/tuning/plan" routerLinkActive="active">Plan de ejecución</a>
      <a routerLink="/tuning/statistics" routerLinkActive="active">Estadísticas e índices</a>
      <a routerLink="/tuning/monitoring" routerLinkActive="active">Monitoreo</a>
    </nav>
    <router-outlet />
  `,
  styleUrl: './tuning.css',
})
export class Tuning {}
