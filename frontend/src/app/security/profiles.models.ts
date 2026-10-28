/** A profile and its limits. Mirrors `una.dbadmin.security.users.DbProfile`. */
export interface DbProfile {
  name: string;
  limits: { resource: string; resourceType: 'KERNEL' | 'PASSWORD' | string; limit: string }[];
}

/** Limits that can be set from the screen, as accepted by the backend, with their units. */
export const PROFILE_LIMITS: { resource: string; unit: string; type: 'KERNEL' | 'PASSWORD' }[] = [
  { resource: 'SESSIONS_PER_USER', unit: 'sessions', type: 'KERNEL' },
  { resource: 'CPU_PER_SESSION', unit: 'hundredths of a second', type: 'KERNEL' },
  { resource: 'CPU_PER_CALL', unit: 'hundredths of a second', type: 'KERNEL' },
  { resource: 'CONNECT_TIME', unit: 'minutes', type: 'KERNEL' },
  { resource: 'IDLE_TIME', unit: 'minutes', type: 'KERNEL' },
  { resource: 'LOGICAL_READS_PER_SESSION', unit: 'blocks', type: 'KERNEL' },
  { resource: 'LOGICAL_READS_PER_CALL', unit: 'blocks', type: 'KERNEL' },
  { resource: 'FAILED_LOGIN_ATTEMPTS', unit: 'attempts', type: 'PASSWORD' },
  { resource: 'PASSWORD_LIFE_TIME', unit: 'days', type: 'PASSWORD' },
  { resource: 'PASSWORD_REUSE_TIME', unit: 'days', type: 'PASSWORD' },
  { resource: 'PASSWORD_REUSE_MAX', unit: 'changes', type: 'PASSWORD' },
  { resource: 'PASSWORD_LOCK_TIME', unit: 'days', type: 'PASSWORD' },
  { resource: 'PASSWORD_GRACE_TIME', unit: 'days', type: 'PASSWORD' },
];
