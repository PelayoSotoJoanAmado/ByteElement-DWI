import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { LoginCredentials, LoginResponse, ROLES, Session } from './auth.models';

const SESSION_KEY = 'stockguard.session';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly current = signal<Session | null>(this.restore());
  readonly user = computed(() => this.current()?.usuario ?? null);

  login(credentials: LoginCredentials) {
    return this.http.post<LoginResponse>('/api/v1/auth/login', credentials).pipe(
      map((response) => {
        const session: Session = {
          accessToken: response.accessToken,
          expiresAt: Date.now() + response.expiresIn * 1000,
          usuario: response.usuario,
        };
        if (response.tokenType !== 'Bearer' || !this.isValid(session))
          throw new Error('Invalid login response');
        return session;
      }),
      tap((session) => {
        this.current.set(session);
        try {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
        } catch {
          /* In-memory session when storage is unavailable. */
        }
      }),
    );
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
      typeof session.usuario.username === 'string' &&
      typeof session.usuario.id === 'number' &&
      ROLES.includes(session.usuario.rol)
    );
  }
}
