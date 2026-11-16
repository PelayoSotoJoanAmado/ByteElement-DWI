import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
import { Role, User } from './auth.models';
export const PASSWORD_PATH = '/cuenta/cambiar-password';
export function landingPath(role: Role): string {
  return {
    ADMINISTRADOR: '/admin',
    ALMACEN: '/almacen',
    COMPRAS: '/compras',
    SUPERVISOR: '/supervisor',
    CONSULTA_COMERCIAL: '/consulta',
  }[role];
}
export function accountPath(user: User): string {
  return user.requiereCambioPassword ? PASSWORD_PATH : landingPath(user.rol);
}
export function roleGuard(role?: Role): CanActivateFn {
  return () => {
    const router = inject(Router);
    return inject(AuthService)
      .ensureSession()
      .pipe(
        map((session) => {
          if (!session) return router.createUrlTree(['/login']);
          if (session.usuario.requiereCambioPassword) return router.createUrlTree([PASSWORD_PATH]);
          return (
            !role || session.usuario.rol === role || router.createUrlTree(['/acceso-denegado'])
          );
        }),
      );
  };
}
export const adminGuard = roleGuard('ADMINISTRADOR');
export const warehouseGuard = roleGuard('ALMACEN');
export const accountGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .ensureSession()
    .pipe(map((s) => !!s || router.createUrlTree(['/login'])));
};
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .ensureSession()
    .pipe(map((s) => (s ? router.createUrlTree([accountPath(s.usuario)]) : true)));
};
