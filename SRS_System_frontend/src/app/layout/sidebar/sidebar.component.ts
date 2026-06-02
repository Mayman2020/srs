import { CommonModule } from '@angular/common';
import { Component, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';
import { SidebarService } from '../../core/services/sidebar.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { ErpUserProfileStore } from '../../shared/erp/erp-user-profile.store';
import { ErpUserAvatarComponent } from '../../shared/erp/erp-user-avatar.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { ProfileNavigationApiService } from '../../core/api/profile-navigation-api.service';
import type { ShellNavItemDto } from '../../core/api/api-types';
import { AuthApiService } from '../../core/api/auth-api.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, TranslatePipe, ErpUserAvatarComponent, MatTooltipModule, MatIconModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  private readonly sidebarService = inject(SidebarService);
  private readonly profileStore = inject(ErpUserProfileStore);
  private readonly i18n = inject(I18nService);
  private readonly authToken = inject(AuthTokenService);
  private readonly navApi = inject(ProfileNavigationApiService);
  private readonly authApi = inject(AuthApiService);

  readonly collapsed = toSignal(this.sidebarService.collapsed$, { initialValue: false });

  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });

  /** Menu rows from {@code ui_screen} via {@code GET /profile/me/navigation} (permission-filtered). */
  readonly navItems = toSignal(
    this.authToken.session$.pipe(
      switchMap(() => {
        if (!this.authToken.getToken()?.trim()) {
          return of([] as ShellNavItemDto[]);
        }
        return this.navApi.listNav().pipe(
          catchError((err) => {
            // No-silent-HTTP-failures policy: when the user is authenticated but navigation
            // cannot be loaded (e.g. token expired mid-session, server returns 5xx, network drop),
            // log it once so the failure is at least observable in the browser console.
            console.warn('[SidebarComponent] Failed to load /profile/me/navigation', err);
            return of([] as ShellNavItemDto[]);
          })
        );
      })
    ),
    { initialValue: [] as ShellNavItemDto[] }
  );

  isMobile = window.innerWidth <= 1024;

  constructor(public router: Router) {}

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

  /**
   * Prefer i18n keys {@code shellNav.<code>} (ar.json / en.json); fallback to DB labels (UTF-8 Arabic/English).
   */
  navLabel(item: ShellNavItemDto): string {
    const key = `shellNav.${item.code}`;
    const t = this.i18n.instant(key);
    if (t !== key) {
      return t;
    }
    const fallback = this.i18n.currentLang() === 'en' ? item.nameEn : item.nameAr;
    return (fallback?.trim() ? fallback : item.code) ?? item.code;
  }

  isActive(item: ShellNavItemDto): boolean {
    const url = this.router.url.split('?')[0];
    return url === item.routePath || url.startsWith(item.routePath + '/');
  }

  trackByNavCode(_index: number, item: ShellNavItemDto): string {
    return item.code;
  }

  roleLabel(code: string): string {
    const key = `roles.codes.${code}`;
    const t = this.i18n.instant(key);
    return t === key ? code : t;
  }

  logout() {
    this.authApi.logout();
    this.router.navigate(['/login']);
  }
}
