import { Routes } from '@angular/router';
import { Tuning } from './tuning';
import { ExecutionPlan } from './execution-plan';
import { StatisticsIndexes } from './statistics-indexes';
import { Monitoring } from './monitoring';

export const TUNING_ROUTES: Routes = [
  {
    path: '',
    component: Tuning,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'plan' },
      { path: 'plan', component: ExecutionPlan },
      { path: 'statistics', component: StatisticsIndexes },
      { path: 'monitoring', component: Monitoring },
    ],
  },
];
