import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Button } from 'primeng/button';
import { Drawer } from 'primeng/drawer';
import { Menu } from 'primeng/menu';
import { Toast } from 'primeng/toast';
import { MenuItem } from 'primeng/api';

import { version, gitHash } from '../../../environments/version';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, Button, Drawer, Menu, Toast],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
})
export class AppLayoutComponent {
  /** Off-canvas navigation — opened by the hamburger button on mobile. */
  readonly sidebarVisible = signal(false);

  readonly version = version;
  readonly gitHash = gitHash;

  readonly navItems: MenuItem[] = [
    {
      label: 'Dashboard',
      icon: 'pi pi-th-large',
      routerLink: '/dashboard',
      routerLinkActiveOptions: { exact: true },
      command: () => this.closeDrawer(),
    },
  ];

  closeDrawer(): void {
    this.sidebarVisible.set(false);
  }
}
