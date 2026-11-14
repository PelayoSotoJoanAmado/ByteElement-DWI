import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { tap } from 'rxjs';
import { ROLES } from '../../../core/auth/auth.models';
import { AdminApi, Resource, Row } from '../shared/admin-api';
import { RemoteState } from '../shared/remote-state';
import { Column, DataTable } from '../shared/data-table';
import { RecordDetail } from '../shared/record-detail';
import { ROLE_LABELS, display } from '../shared/presentation';
interface Field {
  key: string;
  label: string;
  max: number;
  min?: number;
  type?: string;
  help?: string;
}
const FIELDS: Record<Resource, Field[]> = {
  usuarios: [
    {
      key: 'username',
      label: 'Nombre de usuario',
      max: 80,
      min: 3,
      help: '3 a 80 caracteres: letras, números, puntos, guiones o guion bajo.',
    },
    {
      key: 'password',
      label: 'Contraseña',
      max: 72,
      min: 12,
      type: 'password',
      help: 'Mínimo 12 caracteres y máximo 72 bytes.',
    },
    { key: 'confirm', label: 'Confirmar contraseña', max: 72, type: 'password' },
    { key: 'rol', label: 'Rol', max: 30, type: 'role' },
  ],
  productos: [
    { key: 'sku', label: 'SKU', max: 50 },
    { key: 'categoria', label: 'Categoría', max: 80 },
    { key: 'marca', label: 'Marca', max: 80 },
    { key: 'modelo', label: 'Modelo', max: 120 },
    { key: 'especificacion', label: 'Especificación', max: 500, type: 'textarea' },
    { key: 'critico', label: 'Producto crítico', max: 1, type: 'checkbox' },
  ],
  almacenes: [
    { key: 'codigo', label: 'Código', max: 30 },
    { key: 'nombre', label: 'Nombre', max: 120 },
  ],
};
const COLUMNS: Record<Resource, Column[]> = {
  usuarios: [
    { key: 'username', label: 'Usuario' },
    { key: 'rol', label: 'Rol' },
    { key: 'activo', label: 'Estado', format: 'active' },
  ],
  productos: [
    { key: 'sku', label: 'SKU' },
    { key: 'categoria', label: 'Categoría' },
    { key: 'marca', label: 'Marca' },
    { key: 'modelo', label: 'Modelo' },
    { key: 'critico', label: 'Crítico' },
    { key: 'activo', label: 'Estado', format: 'active' },
  ],
  almacenes: [
    { key: 'codigo', label: 'Código' },
    { key: 'nombre', label: 'Nombre' },
    { key: 'activo', label: 'Estado', format: 'active' },
  ],
};
export const passwordBytes: ValidatorFn = (control) =>
  typeof control.value === 'string' && new TextEncoder().encode(control.value).length > 72
    ? { bytes: true }
    : null;
@Component({
  selector: 'app-admin-resources',
  imports: [ReactiveFormsModule, DataTable, RecordDetail],
  templateUrl: './admin-resources.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminResources {
  private readonly api = inject(AdminApi);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroy = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly records = new RemoteState<Row[]>(this.destroy);
  readonly mutation = new RemoteState<Row>(this.destroy);
  readonly kind = signal<Resource>('usuarios');
  readonly search = signal('');
  readonly status = signal('');
  readonly role = signal('');
  readonly page = signal(0);
  readonly notice = signal('');
  readonly editingId = signal<number | undefined>(undefined);
  readonly selected = signal<Row | null>(null);
  readonly confirming = signal<Row | null>(null);
  readonly showPassword = signal(false);
  readonly editor = viewChild.required<ElementRef<HTMLDialogElement>>('editor');
  readonly detailDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailDialog');
  readonly confirmation = viewChild.required<ElementRef<HTMLDialogElement>>('confirmation');
  readonly roles = ROLES;
  readonly roleLabels = ROLE_LABELS;
  readonly show = display;
  form = new FormGroup<Record<string, FormControl<string | boolean>>>({});
  readonly title = computed(
    () => ({ usuarios: 'Usuarios', productos: 'Productos', almacenes: 'Almacenes' })[this.kind()],
  );
  readonly singular = computed(
    () => ({ usuarios: 'usuario', productos: 'producto', almacenes: 'almacén' })[this.kind()],
  );
  readonly description = computed(
    () =>
      ({
        usuarios: 'Administra las cuentas y los roles de tu equipo.',
        productos: 'Organiza el catálogo de tu inventario.',
        almacenes: 'Mantén tus ubicaciones organizadas.',
      })[this.kind()],
  );
  readonly fields = computed(() => FIELDS[this.kind()]);
  readonly columns = computed(() => COLUMNS[this.kind()]);
  readonly filtered = computed(() =>
    (this.records.data() ?? []).filter((row) => {
      const match = Object.values(row).some((value) =>
        String(value ?? '')
          .toLocaleLowerCase()
          .includes(this.search().toLocaleLowerCase()),
      );
      return (
        match &&
        (!this.status() || row['activo'] === (this.status() === 'activo')) &&
        (!this.role() || row['rol'] === this.role())
      );
    }),
  );
  readonly visible = computed(() => this.filtered().slice(this.page() * 20, this.page() * 20 + 20));
  constructor() {
    this.route.data.pipe(takeUntilDestroyed()).subscribe((data) => {
      this.mutation.clear();
      this.kind.set(data['resource'] as Resource);
      this.page.set(0);
      this.search.set('');
      this.status.set('');
      this.role.set('');
      this.notice.set(
        this.route.snapshot.queryParamMap.has('creado') ? 'Cuenta creada correctamente.' : '',
      );
      this.load();
    });
    afterNextRender(() => {
      if (this.route.snapshot.data['create']) this.openEditor();
    });
  }
  load(): void {
    this.page.set(0);
    this.records.load(this.api.list(this.kind()));
  }
  filter(key: 'search' | 'status' | 'role', event: Event): void {
    this[key].set((event.target as HTMLInputElement).value);
    this.page.set(0);
  }
  openEditor(row?: Row): void {
    this.mutation.clear();
    this.showPassword.set(false);
    this.editingId.set(row ? Number(row['id']) : undefined);
    const controls: Record<string, FormControl<string | boolean>> = {};
    for (const field of this.fields()) {
      const validators =
        field.type === 'checkbox'
          ? []
          : [Validators.required, Validators.maxLength(field.max), Validators.pattern(/\S/)];
      if (field.min) validators.push(Validators.minLength(field.min));
      if (field.key === 'username') validators.push(Validators.pattern(/^[a-zA-Z0-9._-]{3,80}$/));
      if (field.key === 'password') validators.push(passwordBytes);
      controls[field.key] = new FormControl<string | boolean>(
        (row?.[field.key] as string | boolean) ?? (field.type === 'checkbox' ? false : ''),
        { nonNullable: true, validators },
      );
    }
    this.form = new FormGroup(controls, {
      validators:
        this.kind() === 'usuarios'
          ? (group) =>
              group.get('password')?.value === group.get('confirm')?.value
                ? null
                : { mismatch: true }
          : null,
    });
    this.editor().nativeElement.showModal();
    this.changeDetector.markForCheck();
  }
  closeEditor(event?: Event): void {
    if (this.mutation.loading()) {
      event?.preventDefault();
      return;
    }
    this.form.reset();
    this.editor().nativeElement.close();
    if (this.route.snapshot.data['create'])
      void this.router.navigate(['/admin/usuarios'], { replaceUrl: true });
  }
  invalid(key: string): boolean {
    const c = this.form.get(key);
    return !!c?.touched && !!c.invalid;
  }
  save(): void {
    if (this.mutation.loading()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const kind = this.kind();
    const id = this.editingId();
    const payload: Row = {};
    for (const field of this.fields())
      if (field.key !== 'confirm') {
        const value = this.form.controls[field.key].value;
        payload[field.key] =
          typeof value === 'string' && field.key !== 'password' ? value.trim() : value;
      }
    this.mutation.load(
      this.api.save(kind, payload, id).pipe(
        tap(() => {
          this.form.reset();
          this.editor().nativeElement.close();
          this.notice.set(
            id ? 'Cambios guardados correctamente.' : 'Registro creado correctamente.',
          );
          if (this.route.snapshot.data['create'])
            void this.router.navigate(['/admin/usuarios'], {
              queryParams: { creado: '1' },
              replaceUrl: true,
            });
          else this.load();
        }),
      ),
    );
  }
  inspect(row: Row): void {
    this.selected.set(row);
    this.detailDialog().nativeElement.showModal();
  }
  askDeactivate(row: Row): void {
    this.mutation.clear();
    this.confirming.set(row);
    this.confirmation().nativeElement.showModal();
  }
  deactivate(): void {
    const row = this.confirming();
    const kind = this.kind();
    if (!row || kind === 'usuarios' || this.mutation.loading()) return;
    this.mutation.load(
      this.api.deactivate(kind, Number(row['id'])).pipe(
        tap(() => {
          this.confirmation().nativeElement.close();
          this.notice.set('Registro desactivado. Su historial se conserva.');
          this.load();
        }),
      ),
    );
  }
}
