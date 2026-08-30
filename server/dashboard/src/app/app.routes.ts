import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { AppLayoutComponent } from './core/layout/app-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((module) => module.DASHBOARD_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
