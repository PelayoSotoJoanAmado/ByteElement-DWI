import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Icon } from '../../../shared/icon';
@Component({
  selector: 'app-admin-section',
  imports: [RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="pending-section">
    <p class="pending-label">PRÓXIMAMENTE</p>
    <h1>{{ data()['label'] }}</h1>
    <p>Este apartado estará disponible próximamente.</p>
    <p>Mientras tanto, puedes volver al panel de administración.</p>
    <a class="button button-primary" routerLink="/admin"
      >Volver al panel<app-icon name="arrow"
    /></a>
  </section>`,
  styles: `
    .pending-section {
      padding: clamp(28px, 5vw, 65px);
      border: 1px solid #c7dfd1;
      border-radius: 14px;
      background: #fff;
    }
    .pending-label {
      color: #00765b;
      letter-spacing: 2px;
      font-weight: 650;
      font-size: 11px;
    }
    h1 {
      font-family: Georgia, serif;
      font-size: 36px;
      margin: 20px 0;
    }
    p {
      color: #52675b;
      line-height: 1.7;
      font-size: 14px;
    }
    .button {
      margin-top: 20px;
      font-size: 14px;
    }
  `,
})
export class AdminSection {
  readonly data = toSignal(inject(ActivatedRoute).data, { initialValue: { label: '' } });
}
