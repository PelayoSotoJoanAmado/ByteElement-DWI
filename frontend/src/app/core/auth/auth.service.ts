import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, map, Observable, of, shareReplay, tap, timeout } from 'rxjs';
import {
  ChangePassword,
  LoginCredentials,
  LoginResponse,
  ROLES,
  Session,
  User,
} from './auth.models';

const SESSION_KEY = 'stockguard.session';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly current = signal<Session | null>(this.restore());
  readonly user = computed(() => this.current()?.usuario ?? null);

  private verifiedToken: string | null = null;
  private verification?: Observable<Session | null>;

  login(credentials: LoginCredentials) {
    return this.http
      .post<LoginResponse>('/api/v1/auth/login', credentials)
      .pipe(map((response) => this.applyResponse(response)));
  }

  cambiarPassword(payload: ChangePassword) {
    return this.http
      .post<LoginResponse>('/api/v1/auth/cambiar-password', payload)
      .pipe(map((response) => this.applyResponse(response)));
  }

  me() {
    const token = this.session()?.accessToken;
    return this.http.get<User>('/api/v1/auth/me').pipe(
      tap((usuario) => {
        const current = this.session();
        if (current && current.accessToken === token) {
          const updated = { ...current, usuario };
          if (!this.isValid(updated)) {
            this.logout();
            throw new Error('Cuenta inválida');
          }
          this.store(updated);
          this.verifiedToken = token;
        }
      }),
    );
  }

  // Restored browser state is never sufficient to activate a private route.
  ensureSession(): Observable<Session | null> {
    const current = this.session();
    if (!current) return of(null);
    if (this.verifiedToken === current.accessToken) return of(current);
    if (!this.verification) {
      this.verification = this.me().pipe(
        timeout(15000),
        map(() => this.session()),
        catchError(() =>
          of(this.session()?.accessToken !== current.accessToken ? this.session() : null),
        ),
        finalize(() => {
          this.verification = undefined;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.verification;
  }

  requirePasswordChange(): void {
    const current = this.session();
    if (current)
      this.store({ ...current, usuario: { ...current.usuario, requiereCambioPassword: true } });
  }

  private applyResponse(response: LoginResponse): Session {
    const session: Session = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresIn * 1000,
      usuario: response.usuario,
    };
    if (response.tokenType !== 'Bearer' || !this.isValid(session))
      throw new Error('Invalid login response');
    this.store(session);
    this.verifiedToken = session.accessToken;
    return session;
  }

  private store(session: Session): void {
    this.current.set(session);
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      /* memory only */
    }
  }

  session(): Session | null {
    const session = this.current();
    if (session && !this.isValid(session)) {
      this.logout();
      return null;
    }
    return session;
  }

  logout(): void {
    this.verifiedToken = null;
    this.current.set(null);
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* Storage can be blocked by the browser. */
    }
  }

  private restore(): Session | null {
    try {
      const session: unknown = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null');
      if (this.isValid(session)) return session;
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* Malformed or unavailable storage never grants access. */
    }
    return null;
  }

  private isValid(value: unknown): value is Session {
    if (!value || typeof value !== 'object') return false;
    const session = value as Session;
    return (
      typeof session.accessToken === 'string' &&
      session.accessToken.length > 0 &&
      typeof session.expiresAt === 'number' &&
      Number.isFinite(session.expiresAt) &&
      session.expiresAt > Date.now() &&
      !!session.usuario &&
      session.usuario.activo === true &&
      typeof session.usuario.requiereCambioPassword === 'boolean' &&
      Number.isSafeInteger(session.usuario.version) &&
      session.usuario.version >= 0 &&
      typeof session.usuario.username === 'string' &&
      typeof session.usuario.id === 'number' &&
      ROLES.includes(session.usuario.rol)
    );
  }
}
