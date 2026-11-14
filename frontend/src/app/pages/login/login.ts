import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { LoginForm } from './login-form';
@Component({
  selector: 'app-login',
  imports: [RouterLink, Icon, LoginForm],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  readonly year = new Date().getFullYear();
}
