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
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { I18nService, AppLang } from '../../core/i18n/i18n.service';
import { NotificationApiService } from '../../core/api/notification-api.service';
import { AuthApiService } from '../../core/api/auth-api.service';
import { NotificationItemDto } from '../../core/api/api-types';
import { ErpUserProfileStore } from '../../shared/erp/erp-user-profile.store';
import { ErpUserAvatarComponent } from '../../shared/erp/erp-user-avatar.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, TranslatePipe, MatSnackBarModule, ErpUserAvatarComponent],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent implements OnInit {
  @Input() pageTitle!: string;
  @Input() pageSubtitle!: string;
  @Input() actionLabel!: string;
  @Output() action = new EventEmitter<void>();

  @ContentChild('topbarAction') projectedContent?: ElementRef;

  @ViewChild('roleSwitchHost') roleSwitchHost?: ElementRef<HTMLElement>;

  private readonly profileStore = inject(ErpUserProfileStore);

  /** Single source of truth for display name, avatar URL resolution, and roles (see {@link ErpUserProfileStore}). */
  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });

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
    public i18n: I18nService,
    private notificationApi: NotificationApiService,
    private authApi: AuthApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotificationPreview();
  }

  get showRoleSwitcher(): boolean {
    return this.profile().roles.length > 1;
  }

  roleLabel(code: string): string {
    const key = `roles.codes.${code}`;
    const t = this.i18n.instant(key);
    return t === key ? code : t;
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
    const current = this.profile().currentRole;
    if (this.switchingRole || !code || code === current) {
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

  switchLanguage(lang: AppLang): void {
    if (this.i18n.currentLang() === lang) {
      return;
    }
    this.i18n.loadLang(lang).subscribe({
      next: () => {},
      error: () => {
        this.snackBar.open(this.i18n.instant('errors.generic'), this.i18n.instant('common.close'), { duration: 5000 });
      }
    });
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
        this.notifications = this.notifications.map((item, i) =>
          i === index ? { ...item, read: true } : item
        );
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
        this.notifications = this.notifications.filter((_, i) => i !== index);
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

  markAllRead() {
    const unread = this.notifications.filter((n) => !n.read);
    if (!unread.length) {
      return;
    }
    forkJoin(unread.map((n) => this.notificationApi.markRead(n.id))).subscribe({
      next: () => {
        const unreadIds = new Set(unread.map((u) => u.id));
        this.notifications = this.notifications.map((n) =>
          unreadIds.has(n.id) ? { ...n, read: true } : n
        );
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
