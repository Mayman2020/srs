import { Component, DestroyRef, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, interval, map, of, startWith, switchMap } from 'rxjs';
import { AuthApiService } from '../../../core/api/auth-api.service';
import { CurrentUserProfileApiService } from '../../../core/api/current-user-profile-api.service';
import { NotificationApiService } from '../../../core/api/notification-api.service';
import { ProfileNavigationApiService } from '../../../core/api/profile-navigation-api.service';
import { AuthTokenService } from '../../../core/auth/auth-token.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { UiFormatService } from '../../../core/i18n/ui-format.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ThemeService } from '../../../core/services/theme.service';
import { SidebarService } from '../../../core/services/sidebar.service';
import { ErpUserProfileStore } from '../../erp/erp-user-profile.store';
import {
  HeaderComponent,
  HeaderLanguageItem,
  HeaderNotificationItem,
  HeaderRoleItem
} from './header.component';

@Component({
  selector: 'app-header-container',
  standalone: true,
  imports: [HeaderComponent],
  template: `
    <app-header
      [profileName]="profile().displayName"
      [profileRole]="profileRole"
      [profileInitials]="profile().initials"
      [profileAvatarUrl]="profile().avatarPrimarySrc"
      [activeLanguage]="activeLanguage"
      [languages]="languages"
      [notifications]="notifications"
      [unreadCount]="unreadCount"
      [notificationsOpen]="notificationsOpen"
      [isDark]="theme.isDark"
      [submenuXPosition]="submenuXPosition"
      [roleOptions]="roleOptions"
      [roleSwitching]="switchingRole"
      (menuToggle)="toggleSidebar()"
      (notificationsToggle)="toggleNotifications()"
      (notificationsClose)="notificationsOpen = false"
      (notificationSelected)="openNotification($event)"
      (markAllNotificationsRead)="markAllRead()"
      (themeToggle)="toggleTheme()"
      (languageSelected)="selectLanguage($event)"
      (roleSelected)="selectRole($event)"
      (logout)="logout()"></app-header>
  `
})
export class HeaderContainerComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly authApi = inject(AuthApiService);
  private readonly authToken = inject(AuthTokenService);
  private readonly profileStore = inject(ErpUserProfileStore);
  private readonly profileApi = inject(CurrentUserProfileApiService);
  private readonly notificationApi = inject(NotificationApiService);
  private readonly navApi = inject(ProfileNavigationApiService);
  private readonly notification = inject(NotificationService);
  private readonly format = inject(UiFormatService);
  private readonly sidebar = inject(SidebarService);
  readonly i18n = inject(I18nService);
  readonly theme = inject(ThemeService);

  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });

  readonly languages: readonly HeaderLanguageItem[] = this.i18n.languages;
  notifications: HeaderNotificationItem[] = [];
  notificationsOpen = false;
  switchingRole = false;

  constructor() {
    interval(30_000)
      .pipe(
        startWith(0),
        switchMap(() => {
          if (!this.authToken.getToken()?.trim()) {
            return of([]);
          }
          return this.notificationApi.list(0, 8).pipe(
            map((page) => page.content ?? []),
            catchError(() => of([]))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((rows) => {
        this.notifications = rows.map((row) => this.mapNotification(row));
      });
  }

  get activeLanguage(): HeaderLanguageItem {
    return (
      this.languages.find((language) => language.code === this.i18n.currentLang()) ??
      this.languages[0]
    );
  }

  get unreadCount(): number {
    return this.notifications.filter((item) => !item.read).length;
  }

  get profileRole(): string {
    const currentRole = this.profile().currentRole?.trim();
    if (!currentRole) {
      return this.i18n.instant('profile.sidebarSignedIn');
    }
    return this.resolveRoleLabel(currentRole);
  }

  /** Roles the user can switch to (excludes the active role so it does not appear in the menu). */
  get roleOptions(): HeaderRoleItem[] {
    const currentRole = this.profile().currentRole?.trim() ?? '';
    const fallback = currentRole ? [currentRole] : [];
    const codes = [...this.profile().roles, ...fallback].filter((code) => !!code?.trim());
    const uniqueCodes = [...new Set(codes)];
    const switchable = uniqueCodes.filter((code) => code !== currentRole);
    return switchable.map((code) => ({
      code,
      label: this.resolveRoleLabel(code)
    }));
  }

  get submenuXPosition(): 'before' | 'after' {
    return this.i18n.currentDirection === 'rtl' ? 'before' : 'after';
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
  }

  toggleSidebar(): void {
    this.sidebar.toggle();
  }

  openNotification(item: HeaderNotificationItem): void {
    if (!item.read) {
      this.notificationApi
        .markRead(item.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.notifications = this.notifications.map((entry) =>
              entry.id === item.id ? { ...entry, read: true } : entry
            );
          }
        });
    }

    this.notificationsOpen = false;
    if (item.correspondenceId) {
      void this.router.navigate(['/correspondence', item.correspondenceId]);
      return;
    }
    void this.router.navigate(['/notifications']);
  }

  markAllRead(): void {
    const unread = this.notifications.filter((item) => !item.read);
    if (!unread.length) {
      return;
    }
    for (const item of unread) {
      this.notificationApi
        .markRead(item.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
    this.notifications = this.notifications.map((item) => ({ ...item, read: true }));
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.persistUiPreferences();
  }

  selectLanguage(code: string): void {
    if (code !== 'ar' && code !== 'en') {
      return;
    }
    if (code === this.i18n.currentLang()) {
      return;
    }
    this.i18n
      .loadLang(code)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.persistUiPreferences();
        },
        error: (err: unknown) => {
          console.error('[HeaderContainer] language load failed', err);
          this.notification.error('notification.error.general');
        }
      });
  }

  selectRole(roleCode: string): void {
    const current = this.profile().currentRole?.trim();
    if (!roleCode?.trim() || roleCode === current || this.switchingRole) {
      return;
    }

    this.switchingRole = true;
    this.notificationsOpen = false;
    this.authApi
      .switchRole(roleCode)
      .pipe(
        switchMap(() =>
          this.navApi.listNav().pipe(
            map((items) => this.resolvePostRoleSwitchUrl(items)),
            catchError(() => of(this.resolvePostRoleSwitchUrl([])))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (targetUrl) => {
          this.switchingRole = false;
          this.profileApi.refresh();
          void this.navigateAfterRoleSwitch(targetUrl);
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.switchingRole = false;
          const message = err.userMessage?.trim();
          if (message) {
            this.notification.errorRaw(message);
            return;
          }
          this.notification.error('topbar.roleSwitchError');
        }
      });
  }

  private resolvePostRoleSwitchUrl(
    items: readonly { routePath: string }[]
  ): string {
    const currentUrl = this.router.url || '/dashboard';
    const currentPath = this.normalizeRoutePath(currentUrl);
    const allowedRoutes = items
      .map((item) => this.normalizeRoutePath(item.routePath))
      .filter((route): route is string => !!route);

    if (
      currentPath === '/profile' ||
      allowedRoutes.some((route) => this.routeMatches(route, currentPath))
    ) {
      return currentUrl;
    }

    const dashboardRoute = allowedRoutes.find((route) => route === '/dashboard');
    return dashboardRoute ?? allowedRoutes[0] ?? '/profile';
  }

  private normalizeRoutePath(url: string | null | undefined): string {
    const raw = String(url ?? '').trim();
    if (!raw) {
      return '';
    }
    const withoutHash = raw.split('#', 1)[0] ?? raw;
    const withoutQuery = withoutHash.split('?', 1)[0] ?? withoutHash;
    if (!withoutQuery) {
      return '';
    }
    const normalized = withoutQuery.startsWith('/') ? withoutQuery : `/${withoutQuery}`;
    return normalized !== '/' ? normalized.replace(/\/+$/, '') : normalized;
  }

  private routeMatches(routePath: string, currentPath: string): boolean {
    return currentPath === routePath || currentPath.startsWith(`${routePath}/`);
  }

  private async navigateAfterRoleSwitch(targetUrl: string): Promise<void> {
    const normalized = this.normalizeRoutePath(targetUrl) || '/profile';
    const requested = targetUrl.startsWith('/') ? targetUrl : normalized;
    try {
      const ok = await this.router.navigateByUrl(requested);
      if (ok) {
        return;
      }
    } catch {
      // fall through to guarded fallback route
    }
    void this.router.navigateByUrl(normalized);
  }

  logout(): void {
    this.authApi.logout();
    this.notificationsOpen = false;
    void this.router.navigate(['/login']);
  }

  private persistUiPreferences(): void {
    if (!this.authToken.getToken()?.trim()) {
      return;
    }
    this.profileApi
      .updateMyUiPreferences({
        uiTheme: this.theme.mode,
        uiLocale: this.i18n.currentLang()
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          console.error('[HeaderContainer] ui preference save failed', err);
        }
      });
  }

  private mapNotification(row: {
    id: string;
    type: string;
    messageKey: string;
    messageParams: Record<string, unknown> | null;
    read: boolean;
    createdAt: string;
  }): HeaderNotificationItem {
    const params = this.stringifyParams(row.messageParams);
    const message = this.i18n.instant(row.messageKey, params);
    const titleKey = `notifications.type.${row.type}`;
    const title = this.i18n.instant(titleKey);
    return {
      id: row.id,
      title: title === titleKey ? row.type : title,
      message: message === row.messageKey ? row.type : message,
      timeLabel: this.formatTimeLabel(row.createdAt),
      read: row.read,
      correspondenceId: this.extractCorrespondenceId(row.messageParams)
    };
  }

  private stringifyParams(
    raw: Record<string, unknown> | null | undefined
  ): Record<string, string | number> | undefined {
    if (!raw) {
      return undefined;
    }
    const params: Record<string, string | number> = {};
    for (const [key, value] of Object.entries(raw)) {
      if (value === null || value === undefined) {
        continue;
      }
      params[key] = typeof value === 'number' ? value : String(value);
    }
    return Object.keys(params).length ? params : undefined;
  }

  private extractCorrespondenceId(raw: Record<string, unknown> | null | undefined): string | undefined {
    if (!raw) {
      return undefined;
    }
    const value = raw['correspondenceId'];
    if (value === null || value === undefined) {
      return undefined;
    }
    return String(value);
  }

  private formatTimeLabel(value: string): string {
    if (!value) {
      return '';
    }
    return this.format.formatDate(value, 'dd MMM y - hh:mm a');
  }

  private resolveRoleLabel(roleCode: string): string {
    const key = `roles.codes.${roleCode}`;
    const label = this.i18n.instant(key);
    return label === key ? roleCode : label;
  }
}
