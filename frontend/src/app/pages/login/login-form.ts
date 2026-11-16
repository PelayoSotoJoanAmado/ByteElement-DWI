import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, timeout } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { accountPath } from '../../core/auth/auth.guards';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './login-form.html',
  styleUrl: './login-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginForm {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly showPassword = signal(false);
  readonly submitting = signal(false);
  readonly error = signal(this.initialMessage());
  private initialMessage(): string {
    const reason = inject(ActivatedRoute).snapshot.queryParamMap.get('motivo');
    if (reason === 'sin-permiso')
      return 'Tu cuenta no tiene permiso para acceder al panel de administrador.';
    if (reason === 'sesion-vencida')
      return 'Tu sesión finalizó. Inicia sesión de nuevo para continuar.';
    return '';
  }
  readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/\S/)]],
    password: ['', [Validators.required, Validators.maxLength(72), Validators.pattern(/\S/)]],
  });

  invalid(field: 'username' | 'password'): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }

  submit(): void {
    if (this.submitting()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.submitting.set(true);
    // Username is normalized; the password must be sent exactly as entered.
    const { username, password } = this.form.getRawValue();
    this.auth
      .login({ username: username.trim(), password })
      .pipe(
        timeout(15000),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (session) => {
          this.form.reset();
          void this.router.navigateByUrl(accountPath(session.usuario));
        },
        error: (error: unknown) => {
          this.showPassword.set(false);
          this.form.controls.password.reset();
          if (
            error instanceof HttpErrorResponse &&
            (error.status === 401 || error.status === 403)
          ) {
            this.error.set(
              'Usuario o contraseña incorrectos. Verifica tus datos e inténtalo de nuevo.',
            );
          } else if (error instanceof HttpErrorResponse && error.status === 429) {
            this.error.set('Demasiados intentos. Espera un momento antes de volver a intentar.');
          } else {
            this.error.set('No pudimos iniciar sesión. Inténtalo de nuevo en unos momentos.');
          }
        },
      });
  }
}
