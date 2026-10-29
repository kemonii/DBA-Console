export interface PlanStep {
  id: number;
  parentId: number | null;
  depth: number;
  operation: string;
  options: string | null;
  owner: string | null;
  objectName: string | null;
  cost: number | null;
  cardinality: number | null;
  fullTableScan: boolean;
}
export type DataRow = Record<string, string | number | null>;
export type IndexType = 'BTREE' | 'BITMAP';
export type MonitorView =
  | 'instance'
  | 'parameters'
  | 'sessions'
  | 'sessions/programs'
  | 'sessions/users'
  | 'locks'
  | 'locks/objects';
