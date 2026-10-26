import { Routes } from '@angular/router';
import { DataPumpPage } from './backup/data-pump-page';
import { FlashbackPage } from './backup/flashback-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'backup' },
  { path: 'backup', component: DataPumpPage, data: { mode: 'backup' } },
  { path: 'recovery', component: DataPumpPage, data: { mode: 'recovery' } },
  { path: 'flashback', component: FlashbackPage },
];
