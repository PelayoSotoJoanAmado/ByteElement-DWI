import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { landingPath } from '../../core/auth/auth.guards';
import { passwordBytes } from '../admin/resources/admin-resources';
import { errorMessage } from '../admin/shared/remote-state';
@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, RouterLink],
  template: `<main class="account-page">
    <section class="ap-panel">
      <p class="ap-help">STOCKGUARD · MI CUENTA</p>
      <h1>Cambiar contraseña</h1>
      <p>{{ auth.user()?.username }}</p>
      @if (auth.user()?.requiereCambioPassword) {
        <p class="ap-notice">
          Debes cambiar tu contraseña temporal antes de acceder a los módulos.
        </p>
      }
      <p>Mínimo 12 caracteres y máximo 72 bytes UTF-8. La nueva debe ser diferente a la actual.</p>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label class="ap-field"
          >Contraseña actual<input
            class="ap-input"
            type="password"
            autocomplete="current-password"
            formControlName="passwordActual"
        /></label>
        <label class="ap-field"
          >Nueva contraseña<input
            class="ap-input"
            type="password"
            autocomplete="new-password"
            formControlName="passwordNueva"
        /></label>
        <label class="ap-field"
          >Confirmar nueva contraseña<input
            class="ap-input"
            type="password"
            autocomplete="new-password"
            formControlName="confirmacionPassword"
        /></label>
        @if (form.touched && form.invalid) {
          <p class="ap-invalid" role="alert">
            Revisa la longitud, los bytes y la confirmación. No reutilices tu contraseña actual.
          </p>
        }
        @if (error()) {
          <p class="ap-notice ap-error" role="alert">{{ error() }}</p>
        }
        <div class="ap-actions">
          <button class="ap-button ap-primary" [disabled]="busy()">
            {{ busy() ? 'Guardando…' : 'Cambiar contraseña' }}
          </button>
          @if (!auth.user()?.requiereCambioPassword) {
            <a class="ap-button" [routerLink]="home()">Volver al panel</a>
          }
          <button class="ap-button" type="button" (click)="logout()">Cerrar sesión</button>
        </div>
      </form>
    </section>
  </main>`,
  styles: `
    .account-page {
      max-width: 580px;
      margin: 6vh auto;
      padding: 24px;
    }
    .ap-field {
      display: block;
      margin: 18px 0;
    }
    .ap-input {
      display: block;
      width: 100%;
      margin-top: 8px;
    }
  `,
})
export class ChangePassword {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly home = () => landingPath(this.auth.user()!.rol);
  readonly form = inject(FormBuilder).nonNullable.group(
    {
      passwordActual: ['', Validators.required],
      passwordNueva: ['', [Validators.required, Validators.minLength(12), passwordBytes]],
      confirmacionPassword: ['', Validators.required],
    },
    {
      validators: (group) => {
        const v = group.getRawValue();
        return v.passwordNueva !== v.confirmacionPassword || v.passwordActual === v.passwordNueva
          ? { mismatch: true }
          : null;
      },
    },
  );
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.auth
      .cambiarPassword(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (s) => {
          this.form.reset();
          void this.router.navigateByUrl(landingPath(s.usuario.rol));
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
  }
  logout(): void {
    this.form.reset();
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
@Component({
  selector: 'app-access-denied',
  imports: [RouterLink],
  template: `<section class="ap-panel" style="margin:10vh auto;max-width:600px">
    <h1>Acceso denegado</h1>
    <p>Tu cuenta no tiene permiso para realizar esta acción.</p>
    <a [routerLink]="home">Volver a mi panel</a>
  </section>`,
})
export class AccessDenied {
  readonly home = inject(AuthService).user()
    ? landingPath(inject(AuthService).user()!.rol)
    : '/login';
}
