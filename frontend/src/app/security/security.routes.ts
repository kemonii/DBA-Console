import { Routes } from '@angular/router';
import { Security } from './security';
import { Tablespaces } from './tablespaces';

/** Routes of the security module, mounted at `/security`. */
export const SECURITY_ROUTES: Routes = [
  {
    path: '',
    component: Security,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tablespaces' },
      { path: 'tablespaces', component: Tablespaces },
    ],
  },
];
