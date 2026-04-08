import { CommonModule } from '@angular/common';
import { Component, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { toSignal } from '@angular/core/rxjs-interop';
import { SidebarService } from '../../services/sidebar.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { ErpUserProfileStore } from '../../shared/erp/erp-user-profile.store';
import { ErpUserAvatarComponent } from '../../shared/erp/erp-user-avatar.component';

interface NavItem {
  key: string;
  route: string;
  /** i18n key under `nav.*` */
  labelKey: string;
  badgeId?: string;
  icon: SafeHtml;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, TranslatePipe, ErpUserAvatarComponent],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  private readonly sidebarService = inject(SidebarService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly profileStore = inject(ErpUserProfileStore);
  private readonly i18n = inject(I18nService);

  /** Single reactive binding — do not subscribe inside `toggleSidebar()` (that leaked subscriptions). */
  readonly collapsed = toSignal(this.sidebarService.collapsed$, { initialValue: false });

  /** Same {@link ErpUserProfileStore} stream as the topbar — consistent name, avatar, role. */
  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });

  notifications = [
    { id: 1, read: false },
    { id: 2, read: false },
    { id: 3, read: true }
  ];

  isMobile = window.innerWidth <= 1024;

  NAV_ITEMS: NavItem[];

  constructor(public router: Router) {
    this.NAV_ITEMS = [
      {
        key: 'dashboard',
        route: '/dashboard',
        labelKey: 'nav.dashboard',
        icon: this.svg(`
          <path d="M3 13h8V3H3v10zM13 21h8V11h-8v10z
                   M13 3h8v6h-8V3zM3 17h8v4H3v-4z"/>
        `)
      },
      {
        key: 'transactions',
        route: '/transactions',
        labelKey: 'nav.transactions',
        icon: this.svg(`
          <path d="M7 7h10M7 12h10M7 17h10"/>
          <path d="M5 3h14a2 2 0 0 1 2 2v14
                   a2 2 0 0 1-2 2H5
                   a2 2 0 0 1-2-2V5
                   a2 2 0 0 1 2-2z"/>
        `)
      },
      {
        key: 'circulars',
        route: '/circulars',
        labelKey: 'nav.circularInbox',
        icon: this.svg(`
          <path d="M4 4h16v12H4z"/>
          <path d="M8 20h8M12 16v4"/>
        `)
      },
      {
        key: 'users',
        route: '/users',
        labelKey: 'nav.users',
        icon: this.svg(`
          <path d="M16 21v-2a4 4 0 0 0-4-4H5
                   a4 4 0 0 0-4 4v2"/>
          <circle cx="8.5" cy="7" r="4"/>
          <path d="M20 8v6"/>
          <path d="M23 11h-6"/>
        `)
      },
      {
        key: 'roles',
        route: '/roles',
        labelKey: 'nav.roles',
        icon: this.svg(`
          <path d="M12 1l3 5 5 1-3.5 4
                   1 6-5.5-3-5.5 3
                   1-6L4 7l5-1 3-5z"/>
        `)
      },
      {
        key: 'admin',
        route: '/admin-communications-main',
        labelKey: 'nav.adminHub',
        icon: this.svg(`
          <path d="M12 1l3 5 5 1-3.5 4
                   1 6-5.5-3-5.5 3
                   1-6L4 7l5-1 3-5z"/>
          <circle cx="8.5" cy="12" r="1.5"/>
          <circle cx="15.5" cy="12" r="1.5"/>
        `)
      },
      {
        key: 'reports',
        route: '/reports',
        labelKey: 'nav.reports',
        icon: this.svg(`
          <path d="M4 19h16"/>
          <path d="M6 16V8"/>
          <path d="M10 16V4"/>
          <path d="M14 16v-6"/>
          <path d="M18 16v-10"/>
        `)
      }
    ];
  }

  toggleSidebar() {
    this.sidebarService.toggle();
  }

  closeSidebar() {
    this.sidebarService.close();
  }

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 1024;
    this.sidebarService.syncOnResize();
  }

  @HostListener('document:keydown.escape')
  onEsc() {
    this.sidebarService.close();
  }

  private svg(paths: string): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(`
      <svg class="ico" viewBox="0 0 24 24"
           fill="none" stroke="currentColor"
           stroke-width="2" stroke-linecap="round"
           stroke-linejoin="round">
        ${paths}
      </svg>
    `);
  }

  unreadCount(): number {
    return this.notifications.filter((n) => !n.read).length;
  }

  isActive(item: NavItem): boolean {
    const url = this.router.url;
    return url.startsWith(item.route);
  }

  trackByNavKey(_index: number, item: NavItem): string {
    return item.key;
  }

  roleLabel(code: string): string {
    const key = `roles.codes.${code}`;
    const t = this.i18n.instant(key);
    return t === key ? code : t;
  }

  toggleTheme() {
    document.body.classList.toggle('dark');
  }

  logout() {
    this.router.navigate(['/login']);
  }
}
