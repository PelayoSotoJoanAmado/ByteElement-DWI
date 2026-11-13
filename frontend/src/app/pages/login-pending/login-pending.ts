import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
// Public destination until the separately scoped authentication delivery.
@Component({
  selector: 'app-login-pending',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <main class="access-page">
    <a class="brand" routerLink="/" aria-label="Volver a StockGuard"
      ><app-icon name="shield" />StockGuard</a
    >
    <div class="access-card">
      <app-icon name="lock" />
      <p class="access-label">PRÓXIMAMENTE</p>
      <h1>Acceso al sistema</h1>
      <p>Estamos preparando el inicio de sesión para tu cuenta asignada.</p>
      <a class="button button-primary" routerLink="/"
        >Volver a la presentación <app-icon name="arrow"
      /></a>
    </div>
  </main>`,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
      color: white;
      background: #061c29;
    }
    .access-page {
      max-width: 600px;
      margin: auto;
      padding: 48px 24px;
    }
    .access-card {
      padding-top: 70px;
    }
    .access-card > app-icon {
      color: #65f5cd;
      font-size: 40px;
    }
    .access-label {
      font-size: 11px;
      letter-spacing: 2px;
      color: #65f5cd;
      margin-top: 24px;
    }
    h1 {
      font-family: Georgia, serif;
      font-size: 40px;
      margin: 18px 0;
    }
    .access-card > p:not(.access-label) {
      color: #c5d3da;
      line-height: 1.7;
      margin-bottom: 30px;
    }
  `,
})
export class LoginPending {}
