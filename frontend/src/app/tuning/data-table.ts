import { Component, computed, input } from '@angular/core';
import { DataRow } from './tuning.models';

/** Read-only dictionary results, preserving Oracle's column labels. */
@Component({
  selector: 'app-tuning-data-table',
  template: `
    @if (rows().length) {
      <div class="table-scroll" tabindex="0" [attr.aria-label]="caption()">
        <table>
          <caption>
            {{
              caption()
            }}
          </caption>
          <thead>
            <tr>
              @for (column of columns(); track column) {
                <th scope="col">{{ column }}</th>
              }
            </tr>
          </thead>
          <tbody>
            @for (row of rows(); track $index) {
              <tr>
                @for (column of columns(); track column) {
                  <td>{{ row[column] ?? '-' }}</td>
                }
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <p>No rows found.</p>
    }
  `,
  styleUrl: './tuning.css',
})
export class TuningDataTable {
  readonly rows = input.required<DataRow[]>();
  readonly caption = input.required<string>();
  protected readonly columns = computed(() => Object.keys(this.rows()[0] ?? {}));
}
