import { ChangeDetectionStrategy, Component, input } from '@angular/core';
export type IconName =
  | 'shield'
  | 'byteelement'
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
  | 'alert'
  | 'home'
  | 'users'
  | 'warehouse'
  | 'layers'
  | 'pie'
  | 'image'
  | 'monitor'
  | 'logout'
  | 'user-plus'
  | 'bell'
  | 'boxes';
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
      @case ('byteelement') {
        <g transform="translate(0.9 0) scale(0.46)">
          <path fill="currentColor" stroke="none" fill-rule="evenodd"
            d="M24 2 44 13v10q0 3-2 4 2 1 2 4v8L24 50 4 39V13Zm0 5L11 14.25 24 21.5l13-7.25ZM9 19v17l12.5 7V26Zm17.5 7v4L39 23v-4Zm0 10v7L39 36v-7Z" />
        </g>
      }
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
      @case ('home') {
        <path d="m2 10 10-8 10 8M5 8v14h14V8M9 22v-9h6v9" />
      }
      @case ('users') {
        <circle cx="9" cy="6" r="4" />
        <path d="M2 22v-3a7 7 0 0 1 14 0v3M17 3a4 4 0 0 1 0 8M18 15a6 6 0 0 1 4 6" />
      }
      @case ('warehouse') {
        <path d="m2 7 10-5 10 5M3 7v15h18V7ZM7 22V11h10v11M7 15h10M7 18h10M6 7h12" />
      }
      @case ('layers') {
        <path d="m12 2 10 6-10 6L2 8Zm-10 10 10 6 10-6M2 16l10 6 10-6" />
      }
      @case ('pie') {
        <path d="M12 2v10h10A10 10 0 0 0 12 2ZM8 4a10 10 0 1 0 12 12H8Z" />
      }
      @case ('image') {
        <rect x="2" y="3" width="20" height="18" rx="2" />
        <circle cx="8" cy="8" r="2" />
        <path d="m3 20 7-7 4 4 4-6 4 5" />
      }
      @case ('monitor') {
        <rect x="2" y="3" width="20" height="14" rx="2" />
        <path d="M8 22h8M12 17v5" />
      }
      @case ('logout') {
        <path d="M9 3H3v18h6M10 12h12m-5-5 5 5-5 5" />
      }
      @case ('user-plus') {
        <circle cx="9" cy="6" r="4" />
        <path d="M2 22v-3a7 7 0 0 1 14 0v3M20 9v8M16 13h8" />
      }
      @case ('bell') {
        <path d="M5 17V9a7 7 0 0 1 14 0v8l3 2H2ZM9 22h6M12 2V1" />
      }
      @case ('boxes') {
        <path
          d="m12 1 6 3v7l-6 3-6-3V4Zm-6 3 6 3 6-3M12 7v7M6 11l-6 3v7l6 3 6-3v-7M0 14l6 3 6-3M6 17v7M18 11l6 3v7l-6 3-6-3M12 14l6 3 6-3M18 17v7"
        />
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
