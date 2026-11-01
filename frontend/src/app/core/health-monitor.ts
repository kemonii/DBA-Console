import { Service, inject, signal } from '@angular/core';
import { ApiClient } from './api-client';
import { ApiError } from './api-error';

export type ConnectionState = 'checking' | 'connected' | 'disconnected';

/**
 * Tracks whether the backend can reach the Oracle database, using `GET /api/health`.
 */
@Service()
export class HealthMonitor {
  private readonly api = inject(ApiClient);

  private readonly stateSignal = signal<ConnectionState>('checking');
  private readonly detailSignal = signal('');

  readonly state = this.stateSignal.asReadonly();
  /** Message from the last check, useful as a tooltip. */
  readonly detail = this.detailSignal.asReadonly();

  check(): void {
    this.api.get('/health').subscribe({
      next: (result) => {
        this.stateSignal.set(result.ok ? 'connected' : 'disconnected');
        this.detailSignal.set(result.message);
      },
      error: (error: unknown) => {
        this.stateSignal.set('disconnected');
        this.detailSignal.set(error instanceof ApiError ? error.message : 'Error inesperado');
      },
    });
  }
}
