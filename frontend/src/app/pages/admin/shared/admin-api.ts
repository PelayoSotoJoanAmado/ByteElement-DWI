import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';

export type Row = Record<string, unknown>;
export type Params = Record<string, string | number | boolean | null | undefined>;
export interface Page<T = Row> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export interface Product extends Row {
  id: number;
  sku: string;
  categoria: string;
  marca: string;
  modelo: string;
  especificacion: string;
  critico: boolean;
  activo: boolean;
}
export interface Warehouse extends Row {
  id: number;
  codigo: string;
  nombre: string;
  activo: boolean;
}
export type Resource = 'usuarios' | 'productos' | 'almacenes';

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  get<T>(path: string, values: Params = {}): Observable<T> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(values))
      if (value !== '' && value !== null && value !== undefined)
        params = params.set(key, String(value));
    return this.http.get<T>(`/api/v1/${path}`, { params });
  }
  list(resource: Resource): Observable<Row[]> {
    return resource === 'productos' ? this.products() : this.get<Row[]>(resource);
  }
  products(): Observable<Product[]> {
    // The API returns arrays without totals. Continue through every page, never silently truncate selectors or counts.
    return this.get<Product[]>('productos', { pagina: 0, tamanio: 100 }).pipe(
      map((rows) => ({ rows, page: 0 })),
      expand((batch) =>
        batch.rows.length === 100
          ? this.get<Product[]>('productos', { pagina: batch.page + 1, tamanio: 100 }).pipe(
              map((rows) => ({ rows, page: batch.page + 1 })),
            )
          : EMPTY,
      ),
      reduce((all, batch) => [...all, ...batch.rows], [] as Product[]),
    );
  }
  pageRows(path: string, params: Params = {}): Observable<Row[]> {
    return this.get<Page>(path, { ...params, pagina: 0, tamanio: 100 }).pipe(
      expand((page) =>
        page.number + 1 < page.totalPages
          ? this.get<Page>(path, { ...params, pagina: page.number + 1, tamanio: 100 })
          : EMPTY,
      ),
      reduce((all, page) => [...all, ...page.content], [] as Row[]),
    );
  }
  post(path: string, payload: Row): Observable<Row> {
    return this.http.post<Row>(`/api/v1/${path}`, payload);
  }
  save(resource: Resource, payload: Row, id?: number): Observable<Row> {
    return id
      ? this.http.put<Row>(`/api/v1/${resource}/${id}`, payload)
      : this.http.post<Row>(`/api/v1/${resource}`, payload);
  }
  deactivate(resource: 'productos' | 'almacenes', id: number): Observable<Row> {
    return this.http.patch<Row>(`/api/v1/${resource}/${id}/desactivar`, {});
  }
}
