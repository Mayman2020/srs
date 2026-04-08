import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationApiService } from '../../core/api/notification-api.service';
import { NotificationItemDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { forkJoin } from 'rxjs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  date: Date;
  read: boolean;
  type: string;
  correspondenceId?: string;
  important?: boolean;
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, TranslatePipe, MatSnackBarModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class NotificationsComponent implements OnInit {
  notifications: NotificationItem[] = [];
  unreadCount = 0;
  filter: 'all' | 'unread' | 'read' = 'all';
  filteredNotifications: NotificationItem[] = [];

  constructor(
    private notificationApi: NotificationApiService,
    private router: Router,
    private i18n: I18nService,
    private snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: this.notificationApi.list(0, 100),
      next: (page) => {
        const rows = page.content ?? [];
        this.notifications = rows.map((r) => this.mapDto(r));
        this.unreadCount = this.notifications.filter((n) => !n.read).length;
        this.applyFilter();
      },
      error: (err: unknown) => {
        const httpErr = err as HttpErrorResponse & { userMessage?: string };
        console.error('[Notifications] list load failed', httpErr);
        this.notifications = [];
        this.unreadCount = 0;
        this.applyFilter();
        const msg = httpErr.userMessage ?? this.i18n.instant('errors.generic');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 6000 });
      },
    });
  }

  private mapDto(dto: NotificationItemDto): NotificationItem {
    const params = this.stringifyParams(dto.messageParams);
    const message = this.i18n.instant(dto.messageKey, params);
    const correspondenceId = this.extractCorrespondenceId(dto.messageParams);
    const typeKey = `notifications.type.${dto.type}`;
    const typeLabel = this.i18n.instant(typeKey);
    return {
      id: dto.id,
      title: typeLabel === typeKey ? dto.type : typeLabel,
      message: message === dto.messageKey ? this.i18n.instant('notifications.defaultTitle') : message,
      date: dto.createdAt ? new Date(dto.createdAt) : new Date(),
      read: dto.read,
      type: dto.type,
      correspondenceId,
      important: false,
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

  private extractCorrespondenceId(raw: Record<string, unknown> | null | undefined): string | undefined {
    if (!raw) {
      return undefined;
    }
    const v = raw['correspondenceId'];
    if (v === undefined || v === null) {
      return undefined;
    }
    return String(v);
  }

  applyFilter(): void {
    switch (this.filter) {
      case 'unread':
        this.filteredNotifications = this.notifications.filter((n) => !n.read);
        break;
      case 'read':
        this.filteredNotifications = this.notifications.filter((n) => n.read);
        break;
      default:
        this.filteredNotifications = [...this.notifications];
    }
  }

  setFilter(filter: 'all' | 'unread' | 'read'): void {
    this.filter = filter;
    this.applyFilter();
  }

  markAsRead(notification: NotificationItem): void {
    if (notification.read) {
      return;
    }
    this.notificationApi.markRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
        this.applyFilter();
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Notifications] markRead failed', err);
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 5000 });
      },
    });
  }

  markAllAsRead(): void {
    const unread = this.notifications.filter((n) => !n.read);
    if (!unread.length) {
      return;
    }
    forkJoin(unread.map((n) => this.notificationApi.markRead(n.id))).subscribe({
      next: () => {
        this.notifications.forEach((n) => {
          n.read = true;
        });
        this.unreadCount = 0;
        this.applyFilter();
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[Notifications] markAllAsRead failed', err);
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 6000 });
      },
    });
  }

  viewTransaction(correspondenceId?: string): void {
    if (correspondenceId) {
      this.router.navigate(['/transactions', correspondenceId]);
    }
  }

  getNotificationIcon(type: string): string {
    const icons: Record<string, string> = {
      CORRESPONDENCE: '📨',
      WORKFLOW: '✅',
      REMINDER: '⏰',
      SYSTEM: '⚙️',
    };
    return icons[type.toUpperCase()] ?? '📢';
  }

  getNotificationClass(type: string): string {
    const classes: Record<string, string> = {
      CORRESPONDENCE: 'notification-new',
      WORKFLOW: 'notification-success',
      REMINDER: 'notification-warning',
      SYSTEM: 'notification-info',
    };
    return classes[type.toUpperCase()] ?? 'notification-default';
  }

  getTypeLabel(type: string): string {
    const key = `notifications.type.${type}`;
    const s = this.i18n.instant(key);
    return s === key ? type : s;
  }

  getTimeAgo(date: Date): string {
    const diff = Date.now() - new Date(date).getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (minutes < 60) {
      return this.i18n.instant('notifications.agoMinutes', { m: minutes });
    }
    if (hours < 24) {
      return this.i18n.instant('notifications.agoHours', { h: hours });
    }
    return this.i18n.instant('notifications.agoDays', { d: days });
  }

  deleteNotification(id: string, event: Event): void {
    event.stopPropagation();
    this.notificationApi.delete(id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter((n) => n.id !== id);
        this.unreadCount = this.notifications.filter((n) => !n.read).length;
        this.applyFilter();
        this.snackBar.open(
          this.i18n.instant('notifications.deleteSuccess'),
          this.i18n.instant('common.close'),
          { duration: 3000 }
        );
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.snackBar.open(
          err.userMessage ?? this.i18n.instant('errors.generic'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      },
    });
  }
}
