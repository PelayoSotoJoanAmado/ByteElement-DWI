import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminSystem } from './admin-system';
import { AdminApi } from '../shared/admin-api';
function setup(system: string, response: unknown, error = false) {
  TestBed.configureTestingModule({
    imports: [AdminSystem],
    providers: [
      { provide: ActivatedRoute, useValue: { data: of({ system }) } },
      {
        provide: AdminApi,
        useValue: {
          get: () =>
            error ? throwError(() => new HttpErrorResponse({ status: 503 })) : of(response),
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(AdminSystem);
  fixture.detectChanges();
  return fixture;
}
describe('Administrator system status', () => {
  it('shows availability only after a positive response', () => {
    const f = setup('estado', {
      estado: 'UP',
      baseDatos: 'UP',
      consultaMs: 1.2,
      fecha: '2026-11-13T15:00:00Z',
    });
    expect(f.nativeElement.textContent).toContain('Conectada');
    expect(f.nativeElement.textContent).toContain('Disponible');
  });
  it('does not show the service as healthy when the request fails', () => {
    const f = setup('estado', null, true);
    expect(f.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(f.nativeElement.querySelectorAll('.ap-metric').length).toBe(0);
  });
  it('preserves the missing-evidence state without claiming approved tests', () => {
    const f = setup('evidencias', { sinDatos: true, mensaje: 'Sin archivo generado' });
    expect(f.nativeElement.textContent).toContain('Sin evidencias disponibles');
    expect(f.nativeElement.textContent).toContain('Sin archivo generado');
    expect(f.nativeElement.querySelector('app-data-table')).toBeNull();
  });
  it('labels evidence as recorded results and retains failed cases', () => {
    const f = setup('evidencias', {
      sinDatos: false,
      generadoEn: '2026-11-13T10:00:00Z',
      motor: 'H2',
      alcance: 'Solo backend',
      requisitos: [{ id: 'RF01', aprobado: false }],
    });
    expect(f.nativeElement.textContent).toContain('Solo backend');
    expect(f.nativeElement.textContent).toContain('H2');
    expect(f.componentInstance.groups()[0].rows[0]['aprobado']).toBe(false);
  });
});
