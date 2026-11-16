import { Role } from './auth.models';
import { AdminLink, ADMIN_NAVIGATION } from '../../pages/admin/admin.navigation';
import { WAREHOUSE_NAVIGATION } from '../../pages/warehouse/warehouse.navigation';
import { landingPath } from './auth.guards';
export const ROLE_TABS: Record<Role, string[]> = {
  ADMINISTRADOR: [
    'usuarios',
    'productos',
    'almacenes',
    'existencias',
    'movimientos',
    'reposicion',
    'solicitudes',
    'ordenes',
    'cortes',
    'reportes',
    'indicadores',
    'evidencias',
    'estado',
  ],
  ALMACEN: [
    'productos',
    'almacenes',
    'existencias',
    'movimientos',
    'transferencias',
    'conteos',
    'reposicion',
    'solicitudes',
    'ordenes',
    'recepciones',
    'reportes',
  ],
  COMPRAS: [
    'productos',
    'almacenes',
    'existencias',
    'movimientos',
    'reposicion',
    'solicitudes',
    'ordenes',
    'reportes',
  ],
  SUPERVISOR: [
    'productos',
    'almacenes',
    'existencias',
    'movimientos',
    'conteos',
    'reposicion',
    'solicitudes',
    'ordenes',
    'politicas',
    'cortes',
    'reportes',
    'indicadores',
    'estado',
  ],
  CONSULTA_COMERCIAL: ['productos', 'almacenes', 'existencias', 'movimientos', 'reportes'],
};
export const TAB_LABELS: Record<string, string> = {
  usuarios: 'Usuarios',
  productos: 'Productos',
  almacenes: 'Almacenes',
  existencias: 'Existencias',
  movimientos: 'Movimientos',
  transferencias: 'Transferencias',
  conteos: 'Conteos físicos',
  reposicion: 'Reposición',
  solicitudes: 'Solicitudes de compra',
  ordenes: 'Órdenes y recepciones',
  recepciones: 'Registrar recepción',
  politicas: 'Políticas min/max',
  cortes: 'Cortes de stock',
  reportes: 'Reportes de inventario',
  indicadores: 'Indicadores I05–I10',
  evidencias: 'Evidencias técnicas',
  estado: 'Estado del sistema',
};
export function navigation(role: Role): { label: string; links: AdminLink[] }[] {
  const home = landingPath(role);
  if (role === 'ADMINISTRADOR')
    return [
      ...ADMIN_NAVIGATION,
      {
        label: 'Control',
        links: [{ label: 'Cortes de stock', path: home + '/cortes', icon: 'layers' }],
      },
    ];
  if (role === 'ALMACEN') return WAREHOUSE_NAVIGATION;
  return [
    {
      label: 'Área de trabajo',
      links: [
        { label: 'Inicio', path: home, icon: 'home' },
        ...ROLE_TABS[role].map((tab) => ({
          label: TAB_LABELS[tab],
          path: home + '/' + tab,
          icon: 'layers' as const,
        })),
      ],
    },
  ];
}
