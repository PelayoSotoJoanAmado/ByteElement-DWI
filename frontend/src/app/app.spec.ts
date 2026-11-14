import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('Public navigation', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient()] }),
  );
  it('loads the public presentation without a session or an Inicio menu item', async () => {
    const harness = await RouterTestingHarness.create('/');
    const page = harness.routeNativeElement!;
    expect(page.querySelector('h1')?.textContent).toContain('Tu inventario');
    expect(page.querySelector('footer')?.textContent).toContain('Todos los derechos reservados');
    const links = [...page.querySelectorAll('nav[aria-label="Navegación principal"] a')];
    expect(links.map((link) => link.textContent?.trim())).toEqual([
      'Funcionalidades',
      'Cómo funciona',
      'Iniciar sesión',
    ]);
  });
  it('opens the login form without public registration', async () => {
    const harness = await RouterTestingHarness.create('/login');
    expect(harness.routeNativeElement?.textContent).toContain('Bienvenido de vuelta');
    expect(harness.routeNativeElement?.querySelector('input[type="password"]')).not.toBeNull();
  });
});
