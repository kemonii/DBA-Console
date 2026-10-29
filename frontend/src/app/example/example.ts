import { JsonPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../core/api-client';
import { ApiError } from '../core/api-error';
import { OperationResult } from '../core/operation-result.model';

/**
 * Reference screen: copy this folder to start a module screen.
 *
 * The pattern in four steps:
 * 1. A signal holds the form model and `form()` derives the fields and validation from it.
 * 2. `onSubmit()` calls the backend through `ApiClient`, never through `HttpClient` directly.
 * 3. The `OperationResult` (or the error message) is stored in signals.
 * 4. The template shows the loading state, the error, or the result.
 *
 * This example sends `GET /api/<path>`, so the default value (`health`) works against the
 * running backend. A real screen would build the request from its own fields instead.
 */
@Component({
  selector: 'app-example',
  imports: [FormField, JsonPipe],
  templateUrl: './example.html',
  styleUrl: './example.css',
})
export class Example {
  private readonly api = inject(ApiClient);

  // 1. Form model and validation. Use '' / 0 / [] as initial values, never null.
  protected readonly model = signal({ path: 'health' });
  protected readonly requestForm = form(this.model, (schema) => {
    required(schema.path, { message: 'Ingrese una ruta de endpoint' });
  });

  // 3. Screen state.
  protected readonly loading = signal(false);
  protected readonly result = signal<OperationResult | null>(null);
  protected readonly errorMessage = signal('');

  protected onSubmit(event: Event): void {
    event.preventDefault();
    // submit() marks the fields as touched and only runs the action when the form is valid.
    // The action must be async.
    submit(this.requestForm, async () => {
      this.loading.set(true);
      this.result.set(null);
      this.errorMessage.set('');
      try {
        // 2. Call the backend. ApiClient adds the /api prefix and raises ApiError on failure.
        this.result.set(await firstValueFrom(this.api.get('/' + this.model().path)));
      } catch (error) {
        this.errorMessage.set(error instanceof ApiError ? error.message : 'Error inesperado');
      } finally {
        this.loading.set(false);
      }
    });
  }
}
