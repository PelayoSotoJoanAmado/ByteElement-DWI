import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('Session boundary', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });
  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });
  it('discards an expired stored session', () => {
    sessionStorage.setItem(
      'stockguard.session',
      JSON.stringify({
        accessToken: 'expired',
        expiresAt: Date.now() - 1,
        usuario: {
          id: 1,
          username: 'test',
          rol: 'ADMINISTRADOR',
          activo: true,
          requiereCambioPassword: false,
          version: 0,
        },
      }),
    );
    expect(TestBed.inject(AuthService).session()).toBeNull();
    expect(sessionStorage.getItem('stockguard.session')).toBeNull();
  });
  it('rejects inactive users and unknown roles in the response', () => {
    const service = TestBed.inject(AuthService);
    let rejected = false;
    service.login({ username: 'test', password: 'test' }).subscribe({
      error: () => {
        rejected = true;
      },
    });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/auth/login')
      .flush({
        accessToken: 'invalid',
        tokenType: 'Bearer',
        expiresIn: 1800,
        usuario: {
          id: 1,
          username: 'test',
          rol: 'UNKNOWN',
          activo: false,
          requiereCambioPassword: false,
          version: 0,
        },
      });
    expect(rejected).toBe(true);
    expect(service.session()).toBeNull();
  });
  it('expires an active session and removes its token', () => {
    const clock = Date.now();
    const now = vi.spyOn(Date, 'now').mockReturnValue(clock);
    const service = TestBed.inject(AuthService);
    service.login({ username: 'test', password: 'test' }).subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/auth/login')
      .flush({
        accessToken: 'active',
        tokenType: 'Bearer',
        expiresIn: 1,
        usuario: {
          id: 1,
          username: 'test',
          rol: 'COMPRAS',
          activo: true,
          requiereCambioPassword: false,
          version: 0,
        },
      });
    now.mockReturnValue(clock + 1001);
    expect(service.session()).toBeNull();
    expect(sessionStorage.getItem('stockguard.session')).toBeNull();
  });
});
