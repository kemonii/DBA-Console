import { Routes } from '@angular/router';
import { Backup } from './backup';

/** Routes of the backup module, mounted at `/backup`. Add child routes here. */
export const BACKUP_ROUTES: Routes = [{ path: '', component: Backup }];
