import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { SidebarService } from '../../services/sidebar.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

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
  imports: [CommonModule, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {



  collapsed = false;

toggleSidebar() {
  this.sidebarService.toggle();
   this.sidebarService.collapsed$.subscribe(val => {
    this.collapsed = val;
  });
}

  readonly userNameKey = 'sidebar.demoUser';
  readonly userRoleKey = 'sidebar.demoRole';

  notifications = [
    { id: 1, read: false },
    { id: 2, read: false },
    { id: 3, read: true }
  ];

    isMobile = window.innerWidth <= 1024;

  NAV_ITEMS: NavItem[];

  constructor(
    public router: Router,
    private sanitizer: DomSanitizer,
    private sidebar: SidebarService,
    private sidebarService: SidebarService
  ) {

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
      // ,
      // {
      //   key: 'notifications',
      //   route: '/notifications',
      //   label: 'الإشعارات',
      //   badgeId: 'badgeNoti',
      //   icon: this.svg(`
      //     <path d="M18 8a6 6 0 10-12 0
      //              c0 7-3 7-3 7h18
      //              s-3 0-3-7"/>
      //     <path d="M13.73 21a2 2 0 01-3.46 0"/>
      //   `)
      // }
    ];
  }





  closeSidebar() {
    this.sidebar.close();
  }

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 1024;
    this.sidebar.syncOnResize();
  }

  @HostListener('document:keydown.escape')
  onEsc() {
    this.sidebar.close();
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
    return this.notifications.filter(n => !n.read).length;
  }

  isActive(item: NavItem): boolean {
    const url = this.router.url;
    return url.startsWith(item.route);
  }

  toggleTheme() {
    document.body.classList.toggle('dark');
  }

  logout() {
    this.router.navigate(['/login'])
  }
}
