import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Row } from './admin-api';
import { display } from './presentation';
export interface Column {
  key: string;
  label: string;
  format?: string;
}
@Component({
  selector: 'app-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="ap-table-scroll">
    <table class="ap-table">
      <caption class="ap-sr-only">
        {{
          caption()
        }}
      </caption>
      <thead>
        <tr>
          @for (column of columns(); track column.key) {
            <th scope="col">{{ column.label }}</th>
          }
          @if (detail() || editable()) {
            <th scope="col">Acciones</th>
          }
        </tr>
      </thead>
      <tbody>
        @for (row of rows(); track $index) {
          <tr>
            @for (column of columns(); track column.key) {
              <td>
                @if (column.format === 'active' || column.format === 'status') {
                  <span class="ap-badge" [class.ap-muted]="row[column.key] === false">{{
                    show(row[column.key], column.format)
                  }}</span>
                } @else {
                  {{ show(row[column.key], column.format) }}
                }
              </td>
            }
            @if (detail() || editable()) {
              <td>
                <div class="ap-row-actions">
                  @if (detail()) {
                    <button
                      type="button"
                      (click)="inspect.emit(row)"
                      [attr.aria-label]="
                        'Ver detalle de ' +
                        show(
                          row['sku'] ??
                            row['username'] ??
                            row['codigo'] ??
                            row['numeroOrden'] ??
                            row['id']
                        )
                      "
                    >
                      Detalle
                    </button>
                  }
                  @if (editable()) {
                    <button type="button" (click)="edit.emit(row)">Editar</button>
                    @if (row['activo'] === true) {
                      <button class="ap-danger-link" type="button" (click)="deactivate.emit(row)">
                        Desactivar
                      </button>
                    }
                  }
                </div>
              </td>
            }
          </tr>
        } @empty {
          <tr>
            <td
              class="ap-empty-cell"
              [attr.colspan]="columns().length + (detail() || editable() ? 1 : 0)"
            >
              <p>No hay resultados para mostrar.</p>
              <span>Prueba con otros filtros o vuelve a consultar.</span>
            </td>
          </tr>
        }
      </tbody>
    </table>
  </div>`,
})
export class DataTable {
  readonly rows = input.required<Row[]>();
  readonly columns = input.required<Column[]>();
  readonly caption = input('Resultados de la consulta');
  readonly detail = input(false);
  readonly editable = input(false);
  readonly inspect = output<Row>();
  readonly edit = output<Row>();
  readonly deactivate = output<Row>();
  readonly show = display;
}
