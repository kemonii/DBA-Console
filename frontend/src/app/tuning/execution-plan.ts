import { Component, inject, signal } from '@angular/core';
import { form, FormField, maxLength, required, submit } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { TuningApi } from './tuning-api';
import { PlanStep } from './tuning.models';

@Component({
  selector: 'app-execution-plan',
  imports: [FormField],
  templateUrl: './execution-plan.html',
  styleUrl: './tuning.css',
})
export class ExecutionPlan {
  private readonly api = inject(TuningApi);
  protected readonly model = signal({ statement: '' });
  protected readonly fields = form(this.model, (s) => {
    required(s.statement);
    maxLength(s.statement, 100000);
  });
  protected readonly rows = signal<PlanStep[] | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected explain(event: Event): void {
    event.preventDefault();
    if (this.busy()) return;
    void submit(this.fields, async () => {
      this.busy.set(true);
      this.error.set('');
      this.rows.set(null);
      try {
        this.rows.set((await firstValueFrom(this.api.explain(this.model().statement))).data ?? []);
      } catch (error) {
        this.error.set(error instanceof Error ? error.message : 'Error inesperado');
      } finally {
        this.busy.set(false);
      }
    });
  }
}
