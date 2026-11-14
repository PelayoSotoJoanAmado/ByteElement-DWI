import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { AdminApi, Page } from '../admin/shared/admin-api';
import { DataTable } from '../admin/shared/data-table';
import { RemoteState } from '../admin/shared/remote-state';
import { QUERY_COLUMNS } from '../admin/queries/query.config';
@Component({
  selector: 'app-warehouse-dashboard',
  imports: [RouterLink, Icon, DataTable],
  templateUrl: './warehouse-dashboard.html',
  styleUrls: ['../admin/dashboard/admin-dashboard.scss', './warehouse.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WarehouseDashboard {
  private readonly api = inject(AdminApi);
  readonly movements = new RemoteState<Page>(inject(DestroyRef));
  readonly columns = QUERY_COLUMNS['movimientos'].filter((c) =>
    ['fecha', 'productoId', 'tipo', 'cantidad'].includes(c.key),
  );
  readonly shortcuts = [
    {
      title: 'Registrar movimiento',
      description: 'Entradas y salidas de productos.',
      path: 'movimientos',
      icon: 'box' as const,
    },
    {
      title: 'Nueva transferencia',
      description: 'Traslada productos entre almacenes.',
      path: 'transferencias',
      icon: 'arrows' as const,
    },
    {
      title: 'Registrar conteo',
      description: 'Registra el inventario físico.',
      path: 'conteos',
      icon: 'layers' as const,
    },
  ];
  constructor() {
    this.refresh();
  }
  refresh(): void {
    // The existing API sorts chronologically. Fetch the last two small pages to show the latest five.
    this.movements.load(
      this.api.get<Page>('movimientos', { pagina: 0, tamanio: 5 }).pipe(
        switchMap((first) => {
          if (first.totalPages <= 1) return of({ ...first, content: [...first.content].reverse() });
          const last = first.totalPages - 1;
          return forkJoin([
            last === 1
              ? of(first)
              : this.api.get<Page>('movimientos', { pagina: last - 1, tamanio: 5 }),
            this.api.get<Page>('movimientos', { pagina: last, tamanio: 5 }),
          ]).pipe(
            map(([previous, latest]) => ({
              ...latest,
              content: [...previous.content, ...latest.content].reverse().slice(0, 5),
            })),
          );
        }),
      ),
    );
  }
}
