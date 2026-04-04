import { Component, ContentChild, ElementRef, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationApiService } from '../../core/api/notification-api.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { AuthApiService } from '../../core/api/auth-api.service';
import { NotificationItemDto } from '../../core/api/api-types';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent implements OnInit {
  @Input() pageTitle!: string;
  @Input() pageSubtitle!: string;
  @Input() actionLabel!: string;
  @Output() action = new EventEmitter<void>();

  @ContentChild('topbarAction') projectedContent?: ElementRef;

  hasProjectedAction = false;

  userName = '';

  isMobile = window.innerWidth <= 1024;

  showNotifications = false;

  notifications: { id: string; type: string; text: string; time: string; read: boolean; important: boolean; correspondenceId?: string }[] =
    [];

  constructor(
    public router: Router,
    private i18n: I18nService,
    private notificationApi: NotificationApiService,
    private tokens: AuthTokenService,
    private authApi: AuthApiService
  ) {}

  ngOnInit(): void {
    this.refreshUser();
    this.loadNotificationPreview();
  }

  private refreshUser(): void {
    this.userName = this.tokens.getUsername()?.trim() || this.i18n.instant('topbar.demoUserName');
  }

  private loadNotificationPreview(): void {
    this.notificationApi.list(0, 8).subscribe({
      next: (page) => {
        const rows = page.content ?? [];
        this.notifications = rows.map((r) => this.mapPreview(r));
      },
      error: () => {
        this.notifications = [];
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
      }
    });
  }

  deleteNotification(_index: number) {
    /* no API */
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
    for (const n of unread) {
      this.notificationApi.markRead(n.id).subscribe({
        next: () => {
          n.read = true;
        }
      });
    }
  }
}
