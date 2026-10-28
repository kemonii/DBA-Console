import { Routes } from '@angular/router';
import { Privileges } from './privileges';
import { Security } from './security';
import { Tablespaces } from './tablespaces';
import { Users } from './users';

/** Routes of the security module, mounted at `/security`. */
export const SECURITY_ROUTES: Routes = [
  {
    path: '',
    component: Security,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tablespaces' },
      { path: 'tablespaces', component: Tablespaces },
      { path: 'users', component: Users },
      { path: 'privileges', component: Privileges },
    ],
  },
];
