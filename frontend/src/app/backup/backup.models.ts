export type Scope = 'TABLE' | 'SCHEMA' | 'FULL';

/** Outcome of an `expdp` or `impdp` run. The password is already masked by the API. */
export interface DataPumpResult {
  success: boolean;
  exitCode: number;
  command: string;
  output: string[];
}

export interface BackupRequest {
  type: Scope;
  schema?: string;
  table?: string;
  directory: string;
  fileName: string;
  includeTables?: string[];
  excludeTables?: string[];
}

export interface RecoveryRequest {
  type: Scope;
  schema?: string;
  table?: string;
  directory: string;
  fileName: string;
  confirmFull?: boolean;
}

export interface FlashbackTableRequest {
  schema: string;
  table: string;
}

export interface FlashbackQueryRequest extends FlashbackTableRequest {
  /** ISO local date-time, e.g. `2026-10-26T10:15:00`. */
  timestamp: string;
}

export type Row = Record<string, unknown>;
