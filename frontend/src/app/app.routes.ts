import { Routes } from '@angular/router';
import { adminGuard, guestGuard } from './core/auth/auth.guards';
import { ADMIN_NAVIGATION } from './pages/admin/admin.navigation';
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'StockGuard | Tu inventario bajo control',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Acceso al sistema | StockGuard',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () => import('./pages/admin/layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Panel de administración | StockGuard',
        data: { label: 'Panel general' },
        loadComponent: () =>
          import('./pages/admin/dashboard/admin-dashboard').then((m) => m.AdminDashboard),
      },
      {
        path: 'usuarios/nuevo',
        title: 'Crear usuario | StockGuard',
        data: { label: 'Crear usuario' },
        loadComponent: () =>
          import('./pages/admin/section/admin-section').then((m) => m.AdminSection),
      },
      ...ADMIN_NAVIGATION.flatMap((group) => group.links)
        .filter((link) => link.path !== '/admin')
        .map((link) => ({
          path: link.path.replace('/admin/', ''),
          title: `${link.label} | StockGuard`,
          data: { label: link.label },
          loadComponent: () =>
            import('./pages/admin/section/admin-section').then((m) => m.AdminSection),
        })),
      { path: '**', redirectTo: '' },
    ],
  },
  { path: '**', redirectTo: '' },
];
