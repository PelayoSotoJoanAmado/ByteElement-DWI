import { Routes } from '@angular/router';
import { adminGuard, guestGuard } from './core/auth/auth.guards';
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
        data: { label: 'Crear usuario', resource: 'usuarios', create: true },
        loadComponent: () =>
          import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
      },
      ...(['usuarios', 'productos', 'almacenes'] as const).map((resource) => ({
        path: resource,
        title: `${resource.charAt(0).toUpperCase() + resource.slice(1)} | StockGuard`,
        data: { label: resource.charAt(0).toUpperCase() + resource.slice(1), resource },
        loadComponent: () =>
          import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
      })),
      ...(['existencias', 'movimientos', 'compras', 'reportes', 'indicadores'] as const).map(
        (query) => ({
          path: query,
          title: `${query.charAt(0).toUpperCase() + query.slice(1)} | StockGuard`,
          data: { label: query.charAt(0).toUpperCase() + query.slice(1), query },
          loadComponent: () =>
            import('./pages/admin/queries/admin-queries').then((m) => m.AdminQueries),
        }),
      ),
      ...(['evidencias', 'estado'] as const).map((system) => ({
        path: system,
        title: `${system === 'estado' ? 'Estado del sistema' : 'Evidencias'} | StockGuard`,
        data: { label: system === 'estado' ? 'Estado del sistema' : 'Evidencias', system },
        loadComponent: () => import('./pages/admin/system/admin-system').then((m) => m.AdminSystem),
      })),
      { path: '**', redirectTo: '' },
    ],
  },
  { path: '**', redirectTo: '' },
];
