/** A profile and its limits. Mirrors `una.dbadmin.security.users.DbProfile`. */
export interface DbProfile {
  name: string;
  limits: { resource: string; resourceType: 'KERNEL' | 'PASSWORD' | string; limit: string }[];
}

/** Limits that can be set from the screen, as accepted by the backend, with their units. */
export const PROFILE_LIMITS: { resource: string; unit: string; type: 'KERNEL' | 'PASSWORD' }[] = [
  { resource: 'SESSIONS_PER_USER', unit: 'sesiones', type: 'KERNEL' },
  { resource: 'CPU_PER_SESSION', unit: 'centésimas de segundo', type: 'KERNEL' },
  { resource: 'CPU_PER_CALL', unit: 'centésimas de segundo', type: 'KERNEL' },
  { resource: 'CONNECT_TIME', unit: 'minutos', type: 'KERNEL' },
  { resource: 'IDLE_TIME', unit: 'minutos', type: 'KERNEL' },
  { resource: 'LOGICAL_READS_PER_SESSION', unit: 'bloques', type: 'KERNEL' },
  { resource: 'LOGICAL_READS_PER_CALL', unit: 'bloques', type: 'KERNEL' },
  { resource: 'FAILED_LOGIN_ATTEMPTS', unit: 'intentos', type: 'PASSWORD' },
  { resource: 'PASSWORD_LIFE_TIME', unit: 'días', type: 'PASSWORD' },
  { resource: 'PASSWORD_REUSE_TIME', unit: 'días', type: 'PASSWORD' },
  { resource: 'PASSWORD_REUSE_MAX', unit: 'cambios', type: 'PASSWORD' },
  { resource: 'PASSWORD_LOCK_TIME', unit: 'días', type: 'PASSWORD' },
  { resource: 'PASSWORD_GRACE_TIME', unit: 'días', type: 'PASSWORD' },
];
