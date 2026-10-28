/** A database user. Mirrors `una.dbadmin.security.users.DbUser`. */
export interface DbUser {
  username: string;
  /** `OPEN`, `LOCKED`, `EXPIRED`, `EXPIRED & LOCKED`, ... */
  accountStatus: string;
  defaultTablespace: string;
  temporaryTablespace: string;
  profile: string;
  /** ISO-8601 date and time, or null when unknown. */
  created: string | null;
}

/** Body of `POST /api/security/users`. */
export interface CreateUserRequest {
  username: string;
  password: string;
  defaultTablespace?: string;
  temporaryTablespace?: string;
  quotaMb?: number;
  unlimitedQuota?: boolean;
}

/** A role. Mirrors `una.dbadmin.security.users.DbRole`. */
export interface DbRole {
  name: string;
  /** Created by Oracle (CONNECT, RESOURCE, DBA, ...): it can be granted but not dropped. */
  oracleMaintained: boolean;
}

/** What a user or role has been granted. Mirrors `una.dbadmin.security.users.GranteePrivileges`. */
export interface GranteePrivileges {
  grantee: string;
  systemPrivileges: { privilege: string; adminOption: boolean }[];
  roles: { role: string; adminOption: boolean; defaultRole: boolean }[];
  objectPrivileges: { owner: string; objectName: string; privilege: string; grantable: boolean }[];
}
