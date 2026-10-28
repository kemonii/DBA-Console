import { Component, DestroyRef, inject } from '@angular/core';
import { HealthMonitor } from '../core/health-monitor';

const CHECK_INTERVAL_MS = 30_000;

/**
 * Shows whether the backend can reach the database. Checks on load, every 30 seconds,
 * and when the user presses "Recheck".
 */
@Component({
  selector: 'app-connection-status',
  template: `
    <span class="status" role="status" [attr.data-state]="health.state()" [title]="health.detail()">
      <span class="dot" aria-hidden="true"></span>
      @switch (health.state()) {
        @case ('connected') {
          Connected
        }
        @case ('disconnected') {
          Disconnected
        }
        @default {
          Checking connection
        }
      }
    </span>
    <button type="button" (click)="health.check()">Recheck</button>
  `,
  styleUrl: './connection-status.css',
})
export class ConnectionStatus {
  protected readonly health = inject(HealthMonitor);

  constructor() {
    this.health.check();
    const timer = setInterval(() => this.health.check(), CHECK_INTERVAL_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}
