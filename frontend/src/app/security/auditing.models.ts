/** The `audit_trail` parameter. Mirrors `una.dbadmin.security.auditing.AuditTrailParameter`. */
export interface AuditTrailParameter {
  value: string;
  /** Whether records go to SYS.AUD$, which the trail views read. */
  enabled: boolean;
}

/** What is being audited. Mirrors `una.dbadmin.security.auditing.AuditOptions`. */
export interface AuditOptions {
  statementOptions: { userName: string | null; option: string; success: string; failure: string }[];
  /** Each action is `success/failure`: `A` by access, `S` by session, `-` not audited. */
  objectOptions: {
    owner: string;
    objectName: string;
    objectType: string;
    insert: string;
    update: string;
    delete: string;
    select: string;
  }[];
}

export type Whenever = 'ALWAYS' | 'SUCCESSFUL' | 'NOT_SUCCESSFUL';

/** Rows of the trail views. Mirror `una.dbadmin.security.auditing.AuditEntries`. */
export interface TrailEntry {
  timestamp: string | null;
  username: string | null;
  actionName: string | null;
  owner: string | null;
  objectName: string | null;
  privilegeUsed: string | null;
  returnCode: number;
}

export interface SessionEntry {
  timestamp: string | null;
  logoffTime: string | null;
  osUsername: string | null;
  username: string | null;
  terminal: string | null;
  outcome: string | null;
}

export interface ObjectEntry {
  timestamp: string | null;
  osUsername: string | null;
  username: string | null;
  terminal: string | null;
  owner: string | null;
  objectName: string | null;
  actionName: string | null;
  outcome: string | null;
}
