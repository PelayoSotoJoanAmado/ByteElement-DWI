import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './auth.models';

export function landingPath(role: Role): string {
  return role === 'ADMINISTRADOR' ? '/admin' : role === 'ALMACEN' ? '/almacen' : '/';
}

export const adminGuard: CanActivateFn = () => {
  const session = inject(AuthService).session();
  const router = inject(Router);
  if (!session) return router.createUrlTree(['/login']);
  return (
    session.usuario.rol === 'ADMINISTRADOR' ||
    router.createUrlTree(['/login'], { queryParams: { motivo: 'sin-permiso' } })
  );
};

export const warehouseGuard: CanActivateFn = () => {
  const session = inject(AuthService).session();
  const router = inject(Router);
  if (!session) return router.createUrlTree(['/login']);
  return (
    session.usuario.rol === 'ALMACEN' || router.createUrlTree([landingPath(session.usuario.rol)])
  );
};

export const guestGuard: CanActivateFn = () => {
  const session = inject(AuthService).session();
  return session && landingPath(session.usuario.rol) !== '/'
    ? inject(Router).createUrlTree([landingPath(session.usuario.rol)])
    : true;
};
