/** Kind of tablespace, as in `DBA_TABLESPACES.CONTENTS`. */
export type TablespaceContents = 'PERMANENT' | 'TEMPORARY' | 'UNDO';

/** Size and usage of a tablespace. Mirrors `una.dbadmin.security.tablespaces.TablespaceUsage`. */
export interface TablespaceUsage {
  name: string;
  contents: TablespaceContents;
  totalMb: number;
  usedMb: number;
  freeMb: number;
  usedPercent: number;
  freePercent: number;
}

/** A datafile of a tablespace. Mirrors `una.dbadmin.security.tablespaces.Datafile`. */
export interface Datafile {
  fileId: number;
  fileName: string;
  sizeMb: number;
  autoextensible: boolean;
  /** Maximum size when it autoextends, 0 otherwise. */
  maxSizeMb: number;
}

/** Body of `POST /api/security/tablespaces`. */
export interface CreateTablespaceRequest {
  name: string;
  fileName: string;
  sizeMb: number;
  nextMb: number;
  /** Omitted for an unlimited maximum size. */
  maxSizeMb?: number;
  /** Sent only for a temporary tablespace. */
  temporary?: boolean;
}
