import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { LoginForm } from './login-form';
import { AuthService } from '../../core/auth/auth.service';

describe('Login', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginForm],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });
  it('validates empty fields without sending credentials and omits the removed block', async () => {
    const fixture = TestBed.createComponent(LoginForm);
    await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#username-error')?.textContent).toContain(
      'Ingresa tu usuario',
    );
    expect(fixture.nativeElement.querySelector('#password-error')?.textContent).toContain(
      'Ingresa tu contraseña',
    );
    expect(fixture.nativeElement.textContent).not.toContain('¿Necesitas una cuenta?');
    expect(fixture.nativeElement.textContent).not.toContain('Acceso seguro según tu rol');
    http.expectNone('/api/v1/auth/login');
  });
  it('toggles password visibility with an accessible button', async () => {
    const fixture = TestBed.createComponent(LoginForm);
    await fixture.whenStable();
    const toggle: HTMLButtonElement = fixture.nativeElement.querySelector('.password-toggle');
    toggle.click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#password').type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar contraseña');
    toggle.click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#password').type).toBe('password');
  });
  it('sends the password unchanged, prevents duplicate requests and stores the returned role', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginForm);
    fixture.componentInstance.form.setValue({
      username: ' almacen.test ',
      password: ' unchanged password ',
    });
    fixture.componentInstance.submit();
    fixture.componentInstance.submit();
    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      username: 'almacen.test',
      password: ' unchanged password ',
    });
    expect(fixture.componentInstance.submitting()).toBe(true);
    request.flush({
      accessToken: 'test-session-token',
      tokenType: 'Bearer',
      expiresIn: 1800,
      usuario: { id: 1, username: 'almacen.test', rol: 'ALMACEN', activo: true },
    });
    expect(TestBed.inject(AuthService).session()?.usuario.rol).toBe('ALMACEN');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
    expect(navigate).toHaveBeenCalledWith('/');
    expect(fixture.componentInstance.submitting()).toBe(false);
  });
  it('shows invalid credentials without disclosing server details or retaining the password', async () => {
    const fixture = TestBed.createComponent(LoginForm);
    fixture.componentInstance.form.setValue({
      username: 'almacen.test',
      password: 'wrong-password',
    });
    fixture.componentInstance.submit();
    http
      .expectOne('/api/v1/auth/login')
      .flush('sensitive server detail', { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Usuario o contraseña incorrectos',
    );
    expect(fixture.nativeElement.textContent).not.toContain('sensitive server detail');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
    expect(TestBed.inject(AuthService).session()).toBeNull();
    expect(fixture.componentInstance.submitting()).toBe(false);
  });
  it('allows retrying after the service is unavailable', () => {
    const fixture = TestBed.createComponent(LoginForm);
    fixture.componentInstance.form.setValue({
      username: 'almacen.test',
      password: 'test-password',
    });
    fixture.componentInstance.submit();
    http.expectOne('/api/v1/auth/login').flush(null, { status: 503, statusText: 'Unavailable' });
    expect(fixture.componentInstance.error()).toContain('Inténtalo de nuevo');
    expect(fixture.componentInstance.submitting()).toBe(false);
    expect(TestBed.inject(AuthService).session()).toBeNull();
  });
});
