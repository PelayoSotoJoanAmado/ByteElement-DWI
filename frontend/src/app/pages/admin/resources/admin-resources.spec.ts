import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminResources, passwordBytes } from './admin-resources';
import { AdminApi, Resource } from '../shared/admin-api';
import { ROLES } from '../../../core/auth/auth.models';
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
          requiereCambioPassword: false,
          version: 0,
        },
      ]),
    ),
    save: vi.fn(() => of({ id: 2 })),
    post: vi.fn(() => of({ id: 2, requiereCambioPassword: true, version: 4 })),
    get: vi.fn(() => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })),
    deactivate: vi.fn(() =>
      of({ id: 1, activo: false, requiereCambioPassword: false, version: 0 }),
    ),
  };
  TestBed.configureTestingModule({
    imports: [AdminResources],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { user: () => ({ id: 99, rol: 'ADMINISTRADOR' }) } },
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
  it.each(ROLES)('creates a %s user and excludes the confirmation field', (role) => {
    const { component, api } = setup('usuarios');
    component.openEditor();
    component.form.setValue({
      username: 'compras.test',
      password: 'Untouched Password!',
      confirm: 'Untouched Password!',
      rol: role,
    });
    component.save();
    expect(api.save).toHaveBeenCalledWith(
      'usuarios',
      { username: 'compras.test', password: 'Untouched Password!', rol: role },
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
  it('keeps user administration in its account detail', () => {
    const { fixture } = setup('usuarios');
    expect(fixture.nativeElement.textContent).not.toContain('Editar');
    expect(fixture.nativeElement.textContent).not.toContain('Desactivar');
  });
  it('sends only supported fields when editing a warehouse', () => {
    const { component, api } = setup('almacenes');
    component.openEditor({
      id: 3,
      codigo: ' A ',
      nombre: ' Principal ',
      activo: false,
      requiereCambioPassword: false,
      version: 0,
    });
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
    component.askDeactivate({
      id: 7,
      sku: 'A',
      activo: true,
      requiereCambioPassword: false,
      version: 0,
    });
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
  it('edits other accounts using the received version, role, status and required reason', () => {
    const { component, api } = setup('usuarios');
    component.openEditor({
      id: 2,
      username: 'other.user',
      rol: 'COMPRAS',
      activo: true,
      version: 7,
    });
    expect(component.form.contains('password')).toBe(false);
    component.form.patchValue({ rol: 'ALMACEN', motivo: 'Cambio autorizado' });
    component.save();
    expect(api.save).toHaveBeenCalledWith(
      'usuarios',
      {
        username: 'other.user',
        rol: 'ALMACEN',
        activo: true,
        motivo: 'Cambio autorizado',
        version: 7,
      },
      2,
    );
  });
  it('requires confirmation and reason before deactivating an account', () => {
    const { component, api } = setup('usuarios');
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.openEditor({
      id: 2,
      username: 'other.user',
      rol: 'COMPRAS',
      activo: true,
      version: 7,
    });
    component.form.patchValue({ activo: false });
    component.save();
    expect(confirm).not.toHaveBeenCalled();
    component.form.patchValue({ motivo: 'Baja temporal' });
    component.save();
    expect(confirm).toHaveBeenCalled();
    expect(api.save).not.toHaveBeenCalled();
    confirm.mockReturnValue(true);
    component.save();
    expect(api.save).toHaveBeenCalledWith(
      'usuarios',
      expect.objectContaining({ activo: false, version: 7, motivo: 'Baja temporal' }),
      2,
    );
    confirm.mockRestore();
  });
  it('does not retry a 409 with an invented version', () => {
    const { component, api } = setup('usuarios');
    api.save.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({ status: 409, error: { detail: 'Version obsoleta; recargue' } }),
      ) as never,
    );
    component.openEditor({
      id: 2,
      username: 'other.user',
      rol: 'COMPRAS',
      activo: true,
      version: 7,
    });
    component.form.patchValue({ motivo: 'Edición autorizada' });
    component.save();
    expect(api.save).toHaveBeenCalledTimes(1);
    expect(component.editingRow()?.['version']).toBe(7);
    expect(component.mutation.error()).toContain('recargue');
  });
  it('prevents opening administrative editing and reset for the current account', () => {
    const { component, api } = setup('usuarios');
    const own = { id: 99, username: 'myself', version: 7 };
    component.openEditor(own);
    component.openEditor(own, true);
    expect(component.editingRow()).toBeNull();
    expect(api.save).not.toHaveBeenCalled();
    expect(api.post).not.toHaveBeenCalled();
  });
  it('resets another account with version and reason and clears the secret fields', () => {
    const { component, api } = setup('usuarios');
    component.openEditor({ id: 2, version: 7 }, true);
    component.form.setValue({
      passwordTemporal: 'Temporary key 123',
      confirmacionPassword: 'Temporary key 123',
      motivo: 'Recuperación solicitada',
    });
    component.save();
    expect(api.post).toHaveBeenCalledWith('usuarios/2/restablecer-password', {
      passwordTemporal: 'Temporary key 123',
      confirmacionPassword: 'Temporary key 123',
      motivo: 'Recuperación solicitada',
      version: 7,
    });
    expect(component.form.get('passwordTemporal')?.value).not.toBe('Temporary key 123');
  });
  it('paginates the selected account audit trail', () => {
    const { component, api } = setup('usuarios');
    component.selected.set({ id: 2 });
    component.loadHistory(1);
    expect(api.get).toHaveBeenCalledWith('usuarios/2/eventos', { pagina: 1, tamanio: 20 });
  });
});
