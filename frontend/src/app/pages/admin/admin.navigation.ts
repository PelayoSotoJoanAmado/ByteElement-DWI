import { IconName } from '../../shared/icon';
export interface AdminLink {
  label: string;
  path: string;
  icon: IconName;
}
export const ADMIN_NAVIGATION: { label: string; links: AdminLink[] }[] = [
  {
    label: 'Administración',
    links: [
      { label: 'Panel general', path: '/admin', icon: 'home' },
      { label: 'Usuarios', path: '/admin/usuarios', icon: 'users' },
      { label: 'Productos', path: '/admin/productos', icon: 'box' },
      { label: 'Almacenes', path: '/admin/almacenes', icon: 'warehouse' },
    ],
  },
  {
    label: 'Consultas',
    links: [
      { label: 'Existencias', path: '/admin/existencias', icon: 'layers' },
      { label: 'Movimientos', path: '/admin/movimientos', icon: 'arrows' },
      { label: 'Compras', path: '/admin/compras', icon: 'cart' },
      { label: 'Reportes', path: '/admin/reportes', icon: 'chart' },
      { label: 'Indicadores', path: '/admin/indicadores', icon: 'pie' },
    ],
  },
  {
    label: 'Sistema',
    links: [
      { label: 'Evidencias', path: '/admin/evidencias', icon: 'image' },
      { label: 'Estado del sistema', path: '/admin/estado', icon: 'monitor' },
    ],
  },
];
