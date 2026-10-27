import { Routes } from '@angular/router';
import { BackupFiles } from './backup-files';
import { Directories } from './directories';
import { Files } from './files';

/** Routes of the files module, mounted at `/files`. */
export const FILES_ROUTES: Routes = [
  {
    path: '',
    component: Files,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'directories' },
      { path: 'directories', component: Directories },
      { path: 'directories/:name/files', component: BackupFiles },
      { path: 'browse', component: BackupFiles },
    ],
  },
];
