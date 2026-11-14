import { DestroyRef, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subscription, finalize, timeout } from 'rxjs';

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return 'No se pudo conectar con el servicio. Comprueba tu conexión e inténtalo de nuevo.';
    if (error.status === 401) return 'Tu sesión finalizó. Inicia sesión de nuevo.';
    if (error.status === 403) return 'Tu cuenta no tiene permiso para esta operación.';
    if (error.status === 404) return 'No se encontró la información solicitada.';
    const detail: unknown = error.error?.detail;
    if ([400, 409].includes(error.status) && typeof detail === 'string') return detail;
    if (error.status === 409) return 'La operación entra en conflicto con los datos existentes.';
  }
  return 'No se pudo completar la operación. Inténtalo de nuevo.';
}
export class RemoteState<T> {
  readonly data = signal<T | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  private subscription?: Subscription;
  constructor(destroy: DestroyRef) {
    destroy.onDestroy(() => this.subscription?.unsubscribe());
  }
  clear(): void {
    this.subscription?.unsubscribe();
    this.data.set(null);
    this.error.set('');
    this.loading.set(false);
  }
  load(source: Observable<T>): void {
    this.clear();
    this.loading.set(true);
    this.subscription = source
      .pipe(
        timeout(30000),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (data) => this.data.set(data),
        error: (error) => this.error.set(errorMessage(error)),
      });
  }
}
