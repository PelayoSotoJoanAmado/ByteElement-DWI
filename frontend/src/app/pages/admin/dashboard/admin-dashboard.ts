import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon, IconName } from '../../../shared/icon';
@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink, Icon],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboard {
  readonly summaries: { label: string; icon: IconName }[] = [
    { label: 'Usuarios activos', icon: 'users' },
    { label: 'Productos activos', icon: 'box' },
    { label: 'Almacenes activos', icon: 'warehouse' },
    { label: 'Alertas de reposición', icon: 'bell' },
  ];
  readonly shortcuts: {
    title: string;
    description: string;
    link: string;
    label: string;
    icon: IconName;
  }[] = [
    {
      title: 'Gestionar usuarios',
      description: 'Crea cuentas y asigna roles a tu equipo.',
      link: '/admin/usuarios',
      label: 'Ir a usuarios',
      icon: 'users',
    },
    {
      title: 'Administrar productos',
      description: 'Organiza el catálogo de tu inventario.',
      link: '/admin/productos',
      label: 'Ir a productos',
      icon: 'box',
    },
    {
      title: 'Administrar almacenes',
      description: 'Mantén tus ubicaciones organizadas.',
      link: '/admin/almacenes',
      label: 'Ir a almacenes',
      icon: 'warehouse',
    },
  ];
  readonly supervision: { title: string; description: string; link: string; icon: IconName }[] = [
    {
      title: 'Compras y reposición',
      description: 'Consulta solicitudes y órdenes',
      link: '/admin/compras',
      icon: 'cart',
    },
    {
      title: 'Reportes e indicadores',
      description: 'Revisa los resultados del inventario',
      link: '/admin/indicadores',
      icon: 'chart',
    },
    {
      title: 'Estado del sistema',
      description: 'Consulta la disponibilidad del servicio',
      link: '/admin/estado',
      icon: 'monitor',
    },
  ];
}
