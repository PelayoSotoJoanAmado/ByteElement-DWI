import { ROLE_TABS, TAB_LABELS } from './core/auth/role-navigation';
import { Role } from './core/auth/auth.models';
import { Routes } from '@angular/router';
import {
  accountGuard,
  adminGuard,
  guestGuard,
  roleGuard,
  warehouseGuard,
} from './core/auth/auth.guards';
export const routes: Routes = [
  {
    path: 'cuenta/cambiar-password',
    canActivate: [accountGuard],
    title: 'Mi cuenta | ByteElement',
    loadComponent: () => import('./pages/account/change-password').then((m) => m.ChangePassword),
  },
  {
    path: 'acceso-denegado',
    canActivate: [accountGuard],
    title: 'Acceso denegado | ByteElement',
    loadComponent: () => import('./pages/account/change-password').then((m) => m.AccessDenied),
  },

  {
    path: '',
    pathMatch: 'full',
    title: 'ByteElement | Tu inventario bajo control',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Acceso al sistema | ByteElement',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () => import('./pages/admin/layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: 'cortes',
        data: { label: 'Cortes de stock', control: 'cortes' },
        loadComponent: () =>
          import('./pages/operations/supervisor-control').then((m) => m.SupervisorControl),
      },
      {
        path: '',
        pathMatch: 'full',
        title: 'Panel de administración | ByteElement',
        data: { label: 'Panel general' },
        loadComponent: () =>
          import('./pages/admin/dashboard/admin-dashboard').then((m) => m.AdminDashboard),
      },
      {
        path: 'usuarios/nuevo',
        title: 'Crear usuario | ByteElement',
        data: { label: 'Crear usuario', resource: 'usuarios', create: true },
        loadComponent: () =>
          import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
      },
      ...(['usuarios', 'productos', 'almacenes'] as const).map((resource) => ({
        path: resource,
        title: `${resource.charAt(0).toUpperCase() + resource.slice(1)} | ByteElement`,
        data: { label: resource.charAt(0).toUpperCase() + resource.slice(1), resource },
        loadComponent: () =>
          import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
      })),
      ...(['existencias', 'movimientos', 'compras', 'reportes', 'indicadores'] as const).map(
        (query) => ({
          path: query,
          title: `${query.charAt(0).toUpperCase() + query.slice(1)} | ByteElement`,
          data: { label: query.charAt(0).toUpperCase() + query.slice(1), query },
          loadComponent: () =>
            import('./pages/admin/queries/admin-queries').then((m) => m.AdminQueries),
        }),
      ),
      ...(['evidencias', 'estado'] as const).map((system) => ({
        path: system,
        title: `${system === 'estado' ? 'Estado del sistema' : 'Evidencias'} | ByteElement`,
        data: { label: system === 'estado' ? 'Estado del sistema' : 'Evidencias', system },
        loadComponent: () => import('./pages/admin/system/admin-system').then((m) => m.AdminSystem),
      })),
      { path: '**', redirectTo: '' },
    ],
  },
  {
    path: 'almacen',
    canActivate: [warehouseGuard],
    canActivateChild: [warehouseGuard],
    loadComponent: () => import('./pages/admin/layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Panel de almacén | ByteElement',
        data: { label: 'Panel de almacén' },
        loadComponent: () =>
          import('./pages/warehouse/warehouse-dashboard').then((m) => m.WarehouseDashboard),
      },
      ...(['productos', 'almacenes'] as const).map((resource) => ({
        path: resource,
        data: { label: resource === 'productos' ? 'Productos' : 'Almacenes', resource },
        title: `${resource === 'productos' ? 'Productos' : 'Almacenes'} | ByteElement`,
        loadComponent: () =>
          import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
      })),
      ...(['existencias', 'compras', 'reposicion', 'reportes'] as const).map((query) => ({
        path: query,
        title: `${query.charAt(0).toUpperCase() + query.slice(1)} | ByteElement`,
        data: {
          label: query.charAt(0).toUpperCase() + query.slice(1),
          query: query === 'reposicion' ? 'compras' : query,
          initialTab: query === 'compras' ? 'solicitudes' : 'reposicion',
        },
        loadComponent: () =>
          import('./pages/admin/queries/admin-queries').then((m) => m.AdminQueries),
      })),
      ...(['movimientos', 'transferencias', 'conteos', 'recepciones'] as const).map(
        (operation) => ({
          path: operation,
          canDeactivate: [(component: { canLeave(): boolean }) => component.canLeave()],
          title: `${operation.charAt(0).toUpperCase() + operation.slice(1)} | ByteElement`,
          data: {
            label:
              operation === 'movimientos'
                ? 'Entradas y salidas'
                : operation === 'conteos'
                  ? 'Conteos físicos'
                  : operation.charAt(0).toUpperCase() + operation.slice(1),
            operation,
          },
          loadComponent: () =>
            import('./pages/warehouse/warehouse-operations').then((m) => m.WarehouseOperations),
        }),
      ),
      { path: '**', redirectTo: '' },
    ],
  },
  ...(['COMPRAS', 'SUPERVISOR', 'CONSULTA_COMERCIAL'] as Role[]).map((role) => ({
    path: {
      COMPRAS: 'compras',
      SUPERVISOR: 'supervisor',
      CONSULTA_COMERCIAL: 'consulta',
      ADMINISTRADOR: 'admin',
      ALMACEN: 'almacen',
    }[role],
    canActivate: [roleGuard(role)],
    canActivateChild: [roleGuard(role)],
    loadComponent: () => import('./pages/admin/layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: '',
        pathMatch: 'full' as const,
        redirectTo:
          role === 'COMPRAS'
            ? 'solicitudes'
            : role === 'SUPERVISOR'
              ? 'indicadores'
              : 'existencias',
      },
      ...ROLE_TABS[role].map((tab) => {
        const common = { path: tab, title: `${TAB_LABELS[tab]} | ByteElement` };
        if (tab === 'productos' || tab === 'almacenes')
          return {
            ...common,
            data: { label: TAB_LABELS[tab], resource: tab },
            loadComponent: () =>
              import('./pages/admin/resources/admin-resources').then((m) => m.AdminResources),
          };
        if (['politicas', 'conteos', 'cortes'].includes(tab))
          return {
            ...common,
            data: { label: TAB_LABELS[tab], control: tab },
            loadComponent: () =>
              import('./pages/operations/supervisor-control').then((m) => m.SupervisorControl),
          };
        if (tab === 'estado')
          return {
            ...common,
            data: { label: TAB_LABELS[tab], system: tab },
            loadComponent: () =>
              import('./pages/admin/system/admin-system').then((m) => m.AdminSystem),
          };
        return {
          ...common,
          data: {
            label: TAB_LABELS[tab],
            query: ['reposicion', 'solicitudes', 'ordenes'].includes(tab) ? 'compras' : tab,
            initialTab: tab,
          },
          loadComponent: () =>
            import('./pages/admin/queries/admin-queries').then((m) => m.AdminQueries),
        };
      }),
    ],
  })),
  { path: '**', redirectTo: '' },
];
