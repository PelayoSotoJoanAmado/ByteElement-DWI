import { ChangeDetectionStrategy, Component, input } from '@angular/core';
export type IconName =
  | 'shield'
  | 'box'
  | 'arrows'
  | 'cart'
  | 'chart'
  | 'lock'
  | 'arrow'
  | 'menu'
  | 'close'
  | 'user'
  | 'eye'
  | 'eye-off'
  | 'alert';
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    @switch (name()) {
      @case ('shield') {
        <path d="m12 2 9 4v6c0 5-5.4 8.5-9 10-3.6-1.5-9-5-9-10V6Z" />
        <path d="m12 7 5 2.5v5L12 17l-5-2.5v-5ZM7 9.5l5 2.5 5-2.5M12 12v5" />
      }
      @case ('box') {
        <path d="m12 2 9 5v10l-9 5-9-5V7ZM3 7l9 5 9-5M12 12v10M7.5 4.5l9 5" />
      }
      @case ('arrows') {
        <path d="M3 7h17m-5-5 5 5-5 5M21 17H4m5-5-5 5 5 5" />
      }
      @case ('cart') {
        <path d="M2 3h3l4 13h10l3-10H6M9 16l-1 3h12" />
        <circle cx="10" cy="22" r=".6" />
        <circle cx="19" cy="22" r=".6" />
      }
      @case ('chart') {
        <path d="M3 13h4v9H3ZM10 7h4v15h-4ZM17 2h4v20h-4Z" />
      }
      @case ('lock') {
        <rect x="5" y="10" width="14" height="12" rx="2" />
        <path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3" />
      }
      @case ('arrow') {
        <path d="M4 12h16m-6-6 6 6-6 6" />
      }
      @case ('menu') {
        <path d="M4 6h16M4 12h16M4 18h16" />
      }
      @case ('user') {
        <circle cx="12" cy="7" r="4" />
        <path d="M4 22v-3a8 8 0 0 1 16 0v3" />
      }
      @case ('eye') {
        <path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
      }
      @case ('eye-off') {
        <path
          d="m3 3 18 18M9.5 5.3A10 10 0 0 1 12 5c6 0 10 7 10 7a19 19 0 0 1-3.2 4M6 6.5A22 22 0 0 0 2 12s4 7 10 7a11 11 0 0 0 5-1.3M10 10a3 3 0 0 0 4 4"
        />
      }
      @case ('alert') {
        <path d="m12 3 10 18H2ZM12 9v5M12 17v.2" />
      }
      @case ('close') {
        <path d="m6 6 12 12M6 18 18 6" />
      }
    }
  </svg>`,
})
export class Icon {
  readonly name = input.required<IconName>();
}
