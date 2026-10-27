import { Routes } from '@angular/router';
import { Backup } from './backup';
import { DataPumpPage } from './data-pump-page';
import { FlashbackPage } from './flashback-page';

/** Routes of the backup module, mounted at `/backup`. */
export const BACKUP_ROUTES: Routes = [
  {
    path: '',
    component: Backup,
    children: [
      { path: '', pathMatch: 'full', component: DataPumpPage, data: { mode: 'backup' } },
      { path: 'recovery', component: DataPumpPage, data: { mode: 'recovery' } },
      { path: 'flashback', component: FlashbackPage },
    ],
  },
];
