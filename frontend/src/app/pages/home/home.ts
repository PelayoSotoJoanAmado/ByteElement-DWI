import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon, IconName } from '../../shared/icon';
@Component({
  selector: 'app-home',
  imports: [RouterLink, Icon],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();
  readonly features: { icon: IconName; title: string; description: string }[] = [
    {
      icon: 'box',
      title: 'Inventario en tiempo real',
      description: 'Consulta productos y existencias por almacén.',
    },
    {
      icon: 'cart',
      title: 'Compras y reposición',
      description: 'Organiza solicitudes, aprobaciones y recepciones.',
    },
    {
      icon: 'chart',
      title: 'Indicadores claros',
      description: 'Visualiza el estado de tus operaciones.',
    },
  ];
  closeMenu() {
    this.menuOpen.set(false);
  }
}
