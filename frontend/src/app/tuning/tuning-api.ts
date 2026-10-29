import { Service, inject } from '@angular/core';
import { ApiClient } from '../core/api-client';
import { DataRow, IndexType, MonitorView, PlanStep } from './tuning.models';
const enc = encodeURIComponent;
const base = '/tuning/queries';

@Service()
export class TuningApi {
  private readonly api = inject(ApiClient);
  explain(statement: string) {
    return this.api.post<PlanStep[]>(`${base}/plan`, { statement });
  }
  statistics(schema: string) {
    return this.api.get<DataRow[]>(`${base}/statistics/${enc(schema)}`);
  }
  gather(schema: string, table: string) {
    return this.api.post(
      `${base}/statistics/${enc(schema)}${table ? '/' + enc(table) : ''}/gather`,
      {},
    );
  }
  indexes(schema: string) {
    return this.api.get<DataRow[]>(`${base}/indexes/${enc(schema)}`);
  }
  createIndex(schema: string, table: string, name: string, columns: string[], type: IndexType) {
    return this.api.post(`${base}/indexes`, { schema, table, name, columns, type });
  }
  rebuild(schema: string, index: string) {
    return this.api.post(`${base}/indexes/${enc(schema)}/${enc(index)}/rebuild`, {});
  }
  validate(schema: string, index: string) {
    return this.api.post<DataRow[]>(`${base}/indexes/${enc(schema)}/${enc(index)}/validate`, {});
  }
  drop(schema: string, index: string) {
    return this.api.delete(`${base}/indexes/${enc(schema)}/${enc(index)}?confirm=true`);
  }
  monitor(view: MonitorView) {
    return this.api.get<DataRow[]>(`/tuning/monitoring/${view}`);
  }
  kill(sid: number, serial: number) {
    return this.api.delete(`/tuning/monitoring/sessions/${sid}/${serial}?confirm=true`);
  }
}
