import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  signal,
} from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Icon } from '../../../shared/icon';
import { ADMIN_NAVIGATION } from '../admin.navigation';
import { WAREHOUSE_NAVIGATION } from '../../warehouse/warehouse.navigation';
import { AdminSidebar } from '../sidebar/admin-sidebar';
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, AdminSidebar, Icon],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private leaving = false;
  readonly user = this.auth.user;
  readonly warehouse = this.auth.user()?.rol === 'ALMACEN';
  readonly groups = this.warehouse ? WAREHOUSE_NAVIGATION : ADMIN_NAVIGATION;
  readonly home = this.warehouse ? '/almacen' : '/admin';
  readonly roleLabel = this.warehouse ? 'ALMACÉN' : 'ADMINISTRADOR';
  readonly section = this.warehouse ? 'Operaciones' : 'Administración';
  readonly panelLabel = this.warehouse ? 'Panel de almacén' : 'Panel de administrador';
  readonly year = new Date().getFullYear();
  readonly menuOpen = signal(false);
  readonly pageTitle = signal('Panel general');

  constructor() {
    this.updateTitle();
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.menuOpen.set(false);
        this.updateTitle();
      });
    const expiresAt = this.auth.session()?.expiresAt;
    if (expiresAt) {
      const expiry = setTimeout(
        () => this.endSession(true),
        Math.max(0, Math.min(expiresAt - Date.now(), 2147483647)),
      );
      this.destroyRef.onDestroy(() => clearTimeout(expiry));
    }
    effect(() => {
      if (!this.user() && !this.leaving) this.endSession(true);
    });
  }

  logout(): void {
    this.endSession(false);
  }
  private endSession(expired: boolean): void {
    if (this.leaving) return;
    this.leaving = true;
    this.auth.logout();
    void this.router.navigate(['/login'], {
      queryParams: expired ? { motivo: 'sesion-vencida' } : {},
    });
  }
  private updateTitle(): void {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) route = route.firstChild;
    this.pageTitle.set(route.data['label'] ?? 'Panel general');
  }
}
