import { AdminLink } from '../admin/admin.navigation';
export const WAREHOUSE_NAVIGATION: { label: string; links: AdminLink[] }[] = [
  {
    label: 'Operaciones',
    links: [
      { label: 'Panel de almacén', path: '/almacen', icon: 'home' },
      { label: 'Existencias', path: '/almacen/existencias', icon: 'layers' },
      { label: 'Entradas y salidas', path: '/almacen/movimientos', icon: 'arrows' },
      { label: 'Transferencias', path: '/almacen/transferencias', icon: 'arrows' },
      { label: 'Conteos físicos', path: '/almacen/conteos', icon: 'box' },
      { label: 'Recepciones', path: '/almacen/recepciones', icon: 'cart' },
    ],
  },
  {
    label: 'Consultas',
    links: [
      { label: 'Productos', path: '/almacen/productos', icon: 'box' },
      { label: 'Almacenes', path: '/almacen/almacenes', icon: 'warehouse' },
      { label: 'Reposición', path: '/almacen/reposicion', icon: 'layers' },
      { label: 'Compras', path: '/almacen/compras', icon: 'cart' },
      { label: 'Reportes', path: '/almacen/reportes', icon: 'chart' },
    ],
  },
];
