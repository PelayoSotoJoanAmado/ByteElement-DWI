import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminResources, passwordBytes } from './admin-resources';
import { AdminApi, Resource } from '../shared/admin-api';
import { AuthService } from '../../../core/auth/auth.service';
import { FormControl } from '@angular/forms';
function setup(resource: Resource) {
  const api = {
    list: vi.fn(() =>
      of([
        {
          id: 1,
          username: 'admin.test',
          sku: 'A',
          codigo: 'CENTRAL',
          nombre: 'Central',
          activo: true,
        },
      ]),
    ),
    save: vi.fn(() => of({ id: 2 })),
    deactivate: vi.fn(() => of({ id: 1, activo: false })),
  };
  TestBed.configureTestingModule({
    imports: [AdminResources],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { user: () => ({ rol: 'ADMINISTRADOR' }) } },
      { provide: AdminApi, useValue: api },
      {
        provide: ActivatedRoute,
        useValue: {
          data: of({ resource }),
          snapshot: { data: {}, queryParamMap: convertToParamMap({}) },
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(AdminResources);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, api };
}
describe('Administrator resource forms', () => {
  beforeEach(() => {
    if (!HTMLDialogElement.prototype.showModal)
      HTMLDialogElement.prototype.showModal = function () {
        this.open = true;
      };
    if (!HTMLDialogElement.prototype.close)
      HTMLDialogElement.prototype.close = function () {
        this.open = false;
      };
  });
  it('creates a user with a server role and excludes the confirmation field', () => {
    const { component, api } = setup('usuarios');
    component.openEditor();
    component.form.setValue({
      username: 'compras.test',
      password: 'Untouched Password!',
      confirm: 'Untouched Password!',
      rol: 'COMPRAS',
    });
    component.save();
    expect(api.save).toHaveBeenCalledWith(
      'usuarios',
      { username: 'compras.test', password: 'Untouched Password!', rol: 'COMPRAS' },
      undefined,
    );
    expect(component.form.controls['password'].value).not.toBe('Untouched Password!');
  });
  it('rejects password mismatch, invalid usernames and excessive UTF-8 length', () => {
    const { component, api } = setup('usuarios');
    component.openEditor();
    component.form.setValue({
      username: 'bad user',
      password: 'PasswordTest2026!',
      confirm: 'different',
      rol: 'COMPRAS',
    });
    component.save();
    expect(api.save).not.toHaveBeenCalled();
    expect(passwordBytes(new FormControl('á'.repeat(37)))).toEqual({ bytes: true });
    expect(passwordBytes(new FormControl('á'.repeat(36)))).toBeNull();
  });
  it('does not offer editing or deactivation for user accounts', () => {
    const { fixture } = setup('usuarios');
    expect(fixture.nativeElement.textContent).not.toContain('Editar');
    expect(fixture.nativeElement.textContent).not.toContain('Desactivar');
  });
  it('sends only supported fields when editing a warehouse', () => {
    const { component, api } = setup('almacenes');
    component.openEditor({ id: 3, codigo: ' A ', nombre: ' Principal ', activo: false });
    component.save();
    expect(api.save).toHaveBeenCalledWith('almacenes', { codigo: 'A', nombre: 'Principal' }, 3);
  });
  it('requires a confirmation action before deactivation and preserves errors', () => {
    const { component, api } = setup('productos');
    api.deactivate.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 409, error: { detail: 'No se puede desactivar' } }),
      ) as never,
    );
    component.askDeactivate({ id: 7, sku: 'A', activo: true });
    expect(api.deactivate).not.toHaveBeenCalled();
    component.deactivate();
    expect(api.deactivate).toHaveBeenCalledWith('productos', 7);
    expect(component.mutation.error()).toBe('No se puede desactivar');
  });
  it('prevents duplicate creation while the first request is pending', () => {
    const { component, api } = setup('almacenes');
    const pending = new Subject<{ id: number }>();
    api.save.mockReturnValue(pending as never);
    component.openEditor();
    component.form.setValue({ codigo: 'A', nombre: 'Principal' });
    component.save();
    component.save();
    expect(api.save).toHaveBeenCalledTimes(1);
    pending.next({ id: 1 });
    pending.complete();
  });
});
