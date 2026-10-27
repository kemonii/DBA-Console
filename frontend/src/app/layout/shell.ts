import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ConnectionStatus } from './connection-status';

/** Application frame: header with connection status, navigation menu, and the routed page. */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ConnectionStatus],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {}
