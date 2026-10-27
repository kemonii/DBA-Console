import { Routes } from '@angular/router';

/**
 * One lazy-loaded route file per module, so each person edits only their own folder.
 * The `example` route is a reference screen to copy from.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'backup' },
  {
    path: 'backup',
    loadChildren: () => import('./backup/backup.routes').then((m) => m.BACKUP_ROUTES),
  },
  {
    path: 'tuning',
    loadChildren: () => import('./tuning/tuning.routes').then((m) => m.TUNING_ROUTES),
  },
  {
    path: 'security',
    loadChildren: () => import('./security/security.routes').then((m) => m.SECURITY_ROUTES),
  },
  {
    path: 'files',
    loadChildren: () => import('./files/files.routes').then((m) => m.FILES_ROUTES),
  },
  { path: 'example', loadComponent: () => import('./example/example').then((m) => m.Example) },
];
