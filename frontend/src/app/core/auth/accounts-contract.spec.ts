import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { Role, ROLES, User } from './auth.models';
import { landingPath } from './auth.guards';
import { ChangePassword } from '../../pages/account/change-password';
const user = (rol: Role = 'COMPRAS', pending = false): User => ({
  id: 3,
  username: 'cuenta.prueba',
  rol,
  activo: true,
  requiereCambioPassword: pending,
  version: 2,
});
function stored(rol: Role = 'COMPRAS', pending = false) {
  sessionStorage.setItem(
    'stockguard.session',
    JSON.stringify({
      accessToken: 'old',
      expiresAt: Date.now() + 1800000,
      usuario: user(rol, pending),
    }),
  );
}
const response = (token: string) => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresIn: 3600,
  usuario: { ...user(), version: 3 },
});
const change = {
  passwordActual: 'Actual password!',
  passwordNueva: 'New password 123',
  confirmacionPassword: 'New password 123',
};
describe('V8 accounts with simulated HTTP backend', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });
  it.each(ROLES)(
    'blocks direct module URL for pending %s before any panel request',
    async (role) => {
      stored(role, false);
      const auth = TestBed.inject(AuthService);
      auth.ensureSession().subscribe();
      const me = http.expectOne('/api/v1/auth/me');
      expect(me.request.headers.get('Authorization')).toBe('Bearer old');
      me.flush(user(role, true));
      const harness = await RouterTestingHarness.create(landingPath(role));
      expect(TestBed.inject(Router).url).toBe('/cuenta/cambiar-password');
      expect(harness.routeNativeElement?.textContent).toContain('contraseña temporal');
      expect(harness.routeNativeElement?.querySelector('app-admin-layout')).toBeNull();
      http.expectNone((r) => !r.url.endsWith('/auth/me'));
    },
  );
  it('waits for /me and replaces a locally altered role', () => {
    stored('ADMINISTRADOR');
    let result: unknown = 'waiting';
    TestBed.inject(AuthService)
      .ensureSession()
      .subscribe((s) => (result = s?.usuario.rol));
    expect(result).toBe('waiting');
    http.expectOne('/api/v1/auth/me').flush(user('ALMACEN'));
    expect(result).toBe('ALMACEN');
  });
  it('replaces the entire session after password rotation, without storing passwords', () => {
    stored('COMPRAS', true);
    const auth = TestBed.inject(AuthService);
    const oldExpiry = auth.session()!.expiresAt;
    auth.cambiarPassword({ ...change, passwordActual: ' original password ' }).subscribe();
    const req = http.expectOne('/api/v1/auth/cambiar-password');
    expect(req.request.headers.get('Authorization')).toBe('Bearer old');
    expect(req.request.body.passwordActual).toBe(' original password ');
    req.flush(response('new'));
    expect(auth.session()?.accessToken).toBe('new');
    expect(auth.user()?.requiereCambioPassword).toBe(false);
    expect(auth.user()?.version).toBe(3);
    expect(auth.session()!.expiresAt).toBeGreaterThan(oldExpiry);
    expect(JSON.parse(sessionStorage.getItem('stockguard.session')!)).toEqual(auth.session());
    expect(sessionStorage.getItem('stockguard.session')).not.toContain('passwordActual');
  });
  it('ignores a 401 from before a successful password rotation', () => {
    stored();
    TestBed.inject(HttpClient)
      .get('/api/v1/productos')
      .subscribe({ error: () => {} });
    const old = http.expectOne('/api/v1/productos');
    const auth = TestBed.inject(AuthService);
    auth.cambiarPassword(change).subscribe();
    http.expectOne('/api/v1/auth/cambiar-password').flush(response('new'));
    old.flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.session()?.accessToken).toBe('new');
  });
  it('routes a password-required 403 without logging out', () => {
    stored();
    const nav = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(HttpClient)
      .get('/api/v1/productos')
      .subscribe({ error: () => {} });
    http
      .expectOne('/api/v1/productos')
      .flush({ codigo: 'CAMBIO_PASSWORD_REQUERIDO' }, { status: 403, statusText: 'Forbidden' });
    expect(TestBed.inject(AuthService).user()?.requiereCambioPassword).toBe(true);
    expect(nav).toHaveBeenCalledWith('/cuenta/cambiar-password');
  });
  it('validates mismatch, bytes and password reuse locally', () => {
    stored();
    const c = TestBed.createComponent(ChangePassword).componentInstance;
    for (const v of [
      { ...change, passwordNueva: 'á'.repeat(37), confirmacionPassword: 'á'.repeat(37) },
      {
        ...change,
        passwordNueva: change.passwordActual,
        confirmacionPassword: change.passwordActual,
      },
      { ...change, confirmacionPassword: 'mismatch' },
    ]) {
      c.form.setValue(v);
      c.submit();
      expect(c.form.invalid).toBe(true);
    }
    http.expectNone('/api/v1/auth/cambiar-password');
    expect(TestBed.inject(AuthService).session()).not.toBeNull();
  });
  it('shows a wrong-current-password 400 without losing the session', () => {
    stored();
    const c = TestBed.createComponent(ChangePassword).componentInstance;
    c.form.setValue(change);
    c.submit();
    http
      .expectOne('/api/v1/auth/cambiar-password')
      .flush({ detail: 'Password actual incorrecto' }, { status: 400, statusText: 'Bad Request' });
    expect(c.error()).toBe('Password actual incorrecto');
    expect(TestBed.inject(AuthService).session()).not.toBeNull();
  });
  it('supports voluntary change and clears all password fields', () => {
    stored();
    const nav = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const c = TestBed.createComponent(ChangePassword).componentInstance;
    c.form.setValue(change);
    c.submit();
    http.expectOne('/api/v1/auth/cambiar-password').flush(response('new'));
    expect(c.form.getRawValue()).toEqual({
      passwordActual: '',
      passwordNueva: '',
      confirmacionPassword: '',
    });
    expect(nav).toHaveBeenCalledWith('/compras');
  });
});
