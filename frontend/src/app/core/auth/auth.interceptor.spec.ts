import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('Bearer boundary', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  beforeEach(() => {
    sessionStorage.setItem(
      'stockguard.session',
      JSON.stringify({
        accessToken: 'old-token',
        expiresAt: Date.now() + 1800000,
        usuario: { id: 1, username: 'admin.test', rol: 'ADMINISTRADOR', activo: true },
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });
  it('attaches Bearer only to the relative API', () => {
    client.get('/api/v1/usuarios').subscribe();
    const req = http.expectOne('/api/v1/usuarios');
    expect(req.request.headers.get('Authorization')).toBe('Bearer old-token');
    req.flush([]);
    client.get('https://example.com/api/v1/usuarios').subscribe();
    const external = http.expectOne('https://example.com/api/v1/usuarios');
    expect(external.request.headers.has('Authorization')).toBe(false);
    external.flush([]);
  });
  it('never adds an existing token to login', () => {
    client.post('/api/v1/auth/login', {}).subscribe();
    const req = http.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
  it('clears an invalid session on 401', () => {
    client.get('/api/v1/usuarios').subscribe({ error: () => {} });
    http.expectOne('/api/v1/usuarios').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(TestBed.inject(AuthService).session()).toBeNull();
  });
  it('keeps a valid session after a forbidden action', () => {
    client.get('/api/v1/usuarios').subscribe({ error: () => {} });
    http.expectOne('/api/v1/usuarios').flush(null, { status: 403, statusText: 'Forbidden' });
    expect(TestBed.inject(AuthService).session()?.accessToken).toBe('old-token');
  });
  it('does not clear a newer session when an old request returns 401', () => {
    client.get('/api/v1/usuarios').subscribe({ error: () => {} });
    const old = http.expectOne('/api/v1/usuarios');
    TestBed.inject(AuthService).login({ username: 'test', password: 'test' }).subscribe();
    http
      .expectOne('/api/v1/auth/login')
      .flush({
        accessToken: 'new-token',
        tokenType: 'Bearer',
        expiresIn: 1800,
        usuario: { id: 2, username: 'admin.new', rol: 'ADMINISTRADOR', activo: true },
      });
    old.flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(TestBed.inject(AuthService).session()?.accessToken).toBe('new-token');
  });
});
