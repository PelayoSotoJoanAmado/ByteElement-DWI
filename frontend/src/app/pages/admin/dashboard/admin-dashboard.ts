import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { map } from 'rxjs';
import { AdminApi, Row } from '../shared/admin-api';
import { RemoteState } from '../shared/remote-state';
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
  private readonly api = inject(AdminApi);
  private readonly destroy = inject(DestroyRef);
  readonly counts = [0, 1, 2, 3].map(() => new RemoteState<Row[]>(this.destroy));
  constructor() {
    this.refresh();
  }
  refresh(): void {
    this.counts[0].load(this.api.list('usuarios'));
    this.counts[1].load(this.api.list('productos'));
    this.counts[2].load(this.api.list('almacenes'));
    this.counts[3].load(
      this.api
        .pageRows('reposicion')
        .pipe(map((rows) => rows.filter((row) => row['estado'] !== 'NORMAL'))),
    );
  }
  count(index: number): string {
    const rows = this.counts[index].data();
    return rows === null
      ? '—'
      : new Intl.NumberFormat('es-PE').format(
          index === 3 ? rows.length : rows.filter((row) => row['activo'] === true).length,
        );
  }

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
