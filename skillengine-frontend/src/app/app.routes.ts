import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'explorer' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'explorer',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/explorer/explorer.component').then((m) => m.ExplorerComponent),
  },
  // {
  //   path: 'path-finder',
  //   canActivate: [authGuard],
  //   loadComponent: () =>
  //     import('./pages/path-finder/path-finder.component').then((m) => m.PathFinderComponent),
  // },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin.component').then((m) => m.AdminComponent),
  },
  { path: '**', redirectTo: 'explorer' },
];
