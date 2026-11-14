import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { ADMIN_NAVIGATION } from '../admin.navigation';
@Component({
  selector: 'app-admin-sidebar',
  imports: [RouterLink, RouterLinkActive, Icon],
  templateUrl: './admin-sidebar.html',
  styleUrl: './admin-sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSidebar {
  readonly username = input('Administrador');
  readonly navigate = output<void>();
  readonly signOut = output<void>();
  readonly groups = ADMIN_NAVIGATION;
}
