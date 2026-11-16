import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  // Attach credentials only to this application's API, never an external URL or login request.
  if (!request.url.startsWith('/api/v1/') || request.url.split('?')[0] === '/api/v1/auth/login')
    return next(request);
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.session()?.accessToken;
  const authorized = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(authorized).pipe(
    catchError((error: unknown) => {
      // A late response from an old request must not close a newer session.
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        token &&
        auth.session()?.accessToken === token
      ) {
        auth.logout();
        void router.navigate(['/login'], { queryParams: { motivo: 'sesion-vencida' } });
      }
      if (
        error instanceof HttpErrorResponse &&
        error.status === 403 &&
        token &&
        auth.session()?.accessToken === token
      ) {
        if (error.error?.codigo === 'CAMBIO_PASSWORD_REQUERIDO') {
          auth.requirePasswordChange();
          void router.navigateByUrl('/cuenta/cambiar-password');
        } else void router.navigateByUrl('/acceso-denegado');
      }
      return throwError(() => error);
    }),
  );
};
