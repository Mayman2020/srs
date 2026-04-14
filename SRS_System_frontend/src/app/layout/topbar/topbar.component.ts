import {
  Component,
  ContentChild,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  ViewChild,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { I18nService, LanguageOption } from '../../core/i18n/i18n.service';
import { NotificationApiService } from '../../core/api/notification-api.service';
import { AuthApiService } from '../../core/api/auth-api.service';
import { CurrentUserProfileApiService } from '../../core/api/current-user-profile-api.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { NotificationItemDto } from '../../core/api/api-types';
import { ErpUserProfileStore } from '../../shared/erp/erp-user-profile.store';
import { ThemeService } from '../../core/theme/theme.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TranslatePipe,
    MatSnackBarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule
  ],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent implements OnInit {
  @Input() pageTitle = '';
  @Input() pageSubtitle = '';
  @Input() actionLabel = '';
  @Output() action = new EventEmitter<void>();

  @ContentChild('topbarAction') projectedContent?: ElementRef;

  @ViewChild('roleSwitchHost') roleSwitchHost?: ElementRef<HTMLElement>;
  private readonly profileStore = inject(ErpUserProfileStore);
  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });
  readonly theme = inject(ThemeService);

  toast = { show: false, title: '', message: '' };

  hasProjectedAction = false;

  showRoleMenu = false;

  switchingRole = false;

  isMobile = window.innerWidth <= 1024;

  showNotifications = false;

  notifications: { id: string; type: string; text: string; time: string; read: boolean; important: boolean; correspondenceId?: string }[] =
    [];

  constructor(
    public router: Router,
    private i18n: I18nService,
    private notificationApi: NotificationApiService,
    private authApi: AuthApiService,
    private profileApi: CurrentUserProfileApiService,
    private authToken: AuthTokenService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotificationPreview();
  }

  get languages(): LanguageOption[] {
    return this.i18n.languages;
  }

  get activeLanguage(): LanguageOption {
    return (
      this.i18n.languages.find((l) => l.code === this.i18n.currentLang()) ?? this.i18n.languages[0]
    );
  }

  get submenuXPosition(): 'before' | 'after' {
    return this.i18n.currentDirection === 'rtl' ? 'before' : 'after';
  }

  get themeTooltip(): 'topbar.themeSwitchLight' | 'topbar.themeSwitchDark' {
    return this.theme.isDark ? 'topbar.themeSwitchLight' : 'topbar.themeSwitchDark';
  }

  get profileInitials(): string {
    const name = this.profile().displayName?.trim() || '';
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    return parts
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  languageLabel(language: LanguageOption): string {
    return this.i18n.instant(language.labelKey);
  }

  languageNativeLabel(language: LanguageOption): string {
    return this.i18n.instant(language.nativeLabelKey);
  }

  isActiveLanguage(language: LanguageOption): boolean {
    return this.activeLanguage.code === language.code;
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.persistUiPreferences();
  }

  switchLanguage(language: LanguageOption): void {
    if (language.code === this.i18n.currentLang()) {
      return;
    }
    this.i18n.loadLang(language.code).subscribe({
      next: () => this.persistUiPreferences(),
      error: (err: unknown) => {
        console.error('[Topbar] language load failed', err);
        this.snackBar.open(
          this.i18n.instant('errors.generic'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      }
    });
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
      .subscribe({
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          console.error('[Topbar] UI preferences save failed', err);
        }
      });
  }

  roleLabel(code: string): string {
    const key = `roles.codes.${code}`;
    const t = this.i18n.instant(key);
    return t === key ? code : t;
  }

  get showRoleSwitcher(): boolean {
    return this.switchableRoles.length > 0;
  }

  /** Roles available to switch to (active role is hidden — it is already shown on the chip). */
  get switchableRoles(): string[] {
    const current = this.profile().currentRole?.trim() ?? '';
    const raw = this.profile().roles ?? [];
    const unique = [...new Set(raw.map((r) => r?.trim()).filter((r): r is string => !!r))];
    return unique.filter((code) => code !== current);
  }

  toggleRoleMenu(event: Event): void {
    event.stopPropagation();
    if (!this.showRoleSwitcher || this.switchingRole) {
      return;
    }
    this.showRoleMenu = !this.showRoleMenu;
  }

  selectRole(code: string, event: Event): void {
    event.stopPropagation();
    if (this.switchingRole || !code || code === this.profile().currentRole) {
      this.showRoleMenu = false;
      return;
    }
    this.switchingRole = true;
    this.authApi.switchRole(code).subscribe({
      next: () => {
        this.switchingRole = false;
        this.showRoleMenu = false;
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.switchingRole = false;
        const msg = err.userMessage ?? this.i18n.instant('topbar.roleSwitchError');
        this.showToast(this.i18n.instant('topbar.roleSwitchErrorTitle'), msg);
      }
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: Event): void {
    const host = this.roleSwitchHost?.nativeElement;
    if (!this.showRoleMenu || !host) {
      return;
    }
    if (host.contains(ev.target as Node)) {
      return;
    }
    this.showRoleMenu = false;
  }

  private showToast(title: string, message: string): void {
    this.toast = { show: true, title, message };
    setTimeout(() => {
      this.toast.show = false;
    }, 4500);
  }

  private loadNotificationPreview(): void {
    this.notificationApi.list(0, 8).subscribe({
      next: (page) => {
        const rows = page.content ?? [];
        this.notifications = rows.map((r) => this.mapPreview(r));
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Topbar] notification preview load failed', err);
        this.notifications = [];
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 5000 });
      }
    });
  }

  private mapPreview(dto: NotificationItemDto): {
    id: string;
    type: string;
    text: string;
    time: string;
    read: boolean;
    important: boolean;
    correspondenceId?: string;
  } {
    const params = this.stringifyParams(dto.messageParams);
    const text = this.i18n.instant(dto.messageKey, params);
    const correspondenceId = dto.messageParams?.['correspondenceId'] as string | undefined;
    return {
      id: dto.id,
      type: dto.type,
      text: text === dto.messageKey ? dto.type : text,
      time: dto.createdAt ? new Date(dto.createdAt).toISOString().substring(0, 10) : '',
      read: dto.read,
      important: false,
      correspondenceId
    };
  }

  private stringifyParams(
    raw: Record<string, unknown> | null | undefined
  ): Record<string, string | number> | undefined {
    if (!raw) {
      return undefined;
    }
    const o: Record<string, string | number> = {};
    for (const [k, v] of Object.entries(raw)) {
      if (v === null || v === undefined) {
        continue;
      }
      o[k] = typeof v === 'number' ? v : String(v);
    }
    return Object.keys(o).length ? o : undefined;
  }

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 1024;
  }

  logout(): void {
    this.authApi.logout();
    this.router.navigate(['/login']);
  }

  get unreadNotifications(): number {
    return this.notifications.filter((n) => !n.read).length;
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.loadNotificationPreview();
    }
  }

  toggleRead(index: number) {
    const n = this.notifications[index];
    if (!n || n.read) {
      return;
    }
    this.notificationApi.markRead(n.id).subscribe({
      next: () => {
        n.read = true;
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Topbar] markRead failed', err);
        this.snackBar.open(
          err.userMessage ?? this.i18n.instant('errors.generic'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      }
    });
  }

  deleteNotification(index: number) {
    const n = this.notifications[index];
    if (!n) {
      return;
    }
    this.notificationApi.delete(n.id).subscribe({
      next: () => {
        this.notifications.splice(index, 1);
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Topbar] delete notification failed', err);
        this.snackBar.open(
          err.userMessage ?? this.i18n.instant('errors.generic'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      },
    });
  }

  openNotificationsPage() {
    this.showNotifications = false;
    this.router.navigate(['/notifications']);
  }

  openPopoverItem(
    n: { correspondenceId?: string; read: boolean },
    index: number
  ): void {
    this.toggleRead(index);
    if (n.correspondenceId) {
      this.showNotifications = false;
      this.router.navigate(['/transactions', n.correspondenceId]);
    } else {
      this.openNotificationsPage();
    }
  }

  /**
   * No bulk `mark-all-read` API on backend yet — one PATCH per notification.
   * Batched via {@link forkJoin} for a single error/success surface.
   */
  markAllRead() {
    const unread = this.notifications.filter((n) => !n.read);
    if (!unread.length) {
      return;
    }
    forkJoin(unread.map((n) => this.notificationApi.markRead(n.id))).subscribe({
      next: () => {
        for (const n of unread) {
          n.read = true;
        }
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Topbar] markAllRead failed', err);
        this.snackBar.open(
          err.userMessage ?? this.i18n.instant('errors.generic'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      }
    });
  }
}
