import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { Role } from './auth.models';
import { AdminLayout } from '../../pages/admin/layout/admin-layout';
import { AuthService } from './auth.service';

function storedSession(role: Role, expiresAt = Date.now() + 1800000): void {
  sessionStorage.setItem(
    'stockguard.session',
    JSON.stringify({
      accessToken: 'test-token',
      expiresAt,
      usuario: { id: 1, username: 'admin.test', rol: role, activo: true },
    }),
  );
}
describe('Administrator route protection', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient()] });
  });
  afterEach(() => {
    sessionStorage.clear();
    vi.useRealTimers();
  });

  it('redirects an unauthenticated direct URL to login', async () => {
    await RouterTestingHarness.create('/admin');
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it('also protects nested URLs without a session', async () => {
    await RouterTestingHarness.create('/admin/usuarios/nuevo');
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it.each<Role>(['ALMACEN', 'COMPRAS', 'SUPERVISOR', 'CONSULTA_COMERCIAL'])(
    'rejects the %s role',
    async (role) => {
      storedSession(role);
      const harness = await RouterTestingHarness.create('/admin');
      expect(TestBed.inject(Router).url).toContain('/login?motivo=sin-permiso');
      expect(harness.routeNativeElement?.textContent).toContain('Tu cuenta no tiene permiso');
      expect(harness.routeNativeElement?.querySelector('app-admin-layout')).toBeNull();
    },
  );
  it('opens the panel for an administrator and shows no invented metrics', async () => {
    storedSession('ADMINISTRADOR');
    const harness = await RouterTestingHarness.create('/admin');
    expect(TestBed.inject(Router).url).toBe('/admin');
    expect(harness.routeNativeElement?.textContent).toContain('Bienvenido, administrador');
    expect(harness.routeNativeElement?.querySelectorAll('.summary-caption').length).toBe(4);
    expect(harness.routeNativeElement?.textContent).toContain('admin.test');
  });
  it('routes an existing administrator session away from login', async () => {
    storedSession('ADMINISTRADOR');
    await RouterTestingHarness.create('/login');
    expect(TestBed.inject(Router).url).toBe('/admin');
  });
  it('removes the session on logout and prevents revisiting the panel', async () => {
    storedSession('ADMINISTRADOR');
    const harness = await RouterTestingHarness.create('/admin');
    harness.routeNativeElement?.querySelector<HTMLButtonElement>('.sign-out')?.click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(sessionStorage.getItem('stockguard.session')).toBeNull();
    await harness.navigateByUrl('/admin/existencias');
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it('does not restore an expired session', async () => {
    storedSession('ADMINISTRADOR', Date.now() - 1000);
    await RouterTestingHarness.create('/admin');
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it('closes an open panel when the session expires', () => {
    vi.useFakeTimers();
    storedSession('ADMINISTRADOR', Date.now() + 1000);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AdminLayout);
    fixture.detectChanges();
    vi.advanceTimersByTime(1001);
    expect(TestBed.inject(AuthService).session()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { motivo: 'sesion-vencida' },
    });
  });

  it('keeps unimplemented modules explicit and updates the breadcrumb', async () => {
    storedSession('ADMINISTRADOR');
    const harness = await RouterTestingHarness.create('/admin/usuarios');
    expect(harness.routeNativeElement?.textContent).toContain('PRÓXIMAMENTE');
    expect(harness.routeNativeElement?.querySelector('.breadcrumb strong')?.textContent).toBe(
      'Usuarios',
    );
    await harness.navigateByUrl('/admin/productos');
    expect(harness.routeNativeElement?.querySelector('.pending-section h1')?.textContent).toBe(
      'Productos',
    );
  });
});
