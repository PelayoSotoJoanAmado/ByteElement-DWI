import { Routes } from '@angular/router';
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'StockGuard | Tu inventario bajo control',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'login',
    title: 'Acceso al sistema | StockGuard',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  { path: '**', redirectTo: '' },
];
