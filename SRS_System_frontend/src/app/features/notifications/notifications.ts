import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationApiService } from '../../core/api/notification-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  date: Date;
  read: boolean;
  type: string;
  transactionId?: string;
  important?: boolean;
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
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
    private i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.notificationApi.list().subscribe({
      next: (raw) => {
        this.notifications = (raw ?? [])
          .map((r) => this.normalizeItem(r))
          .filter((n): n is NotificationItem => n !== null);
        this.unreadCount = this.notifications.filter((n) => !n.read).length;
        this.applyFilter();
      },
      error: () => {
        this.notifications = [];
        this.unreadCount = 0;
        this.applyFilter();
      },
    });
  }

  /** Accepts backend DTOs when implemented; ignores unknown shapes safely. */
  private normalizeItem(raw: unknown): NotificationItem | null {
    if (!raw || typeof raw !== 'object') {
      return null;
    }
    const o = raw as Record<string, unknown>;
    const id = o['id'];
    if (id === undefined || id === null) {
      return null;
    }
    const title = String(o['title'] ?? o['subject'] ?? '');
    const message = String(o['message'] ?? o['body'] ?? '');
    const dateRaw = o['createdAt'] ?? o['date'] ?? o['occurredAt'];
    const date =
      dateRaw instanceof Date
        ? dateRaw
        : new Date(typeof dateRaw === 'string' || typeof dateRaw === 'number' ? dateRaw : Date.now());
    const read = Boolean(o['read'] ?? o['readAt']);
    const type = String(o['type'] ?? o['notificationType'] ?? 'system');
    const transactionId = o['transactionId'] ?? o['correspondenceId'];
    return {
      id: String(id),
      title: title || this.i18n.instant('notifications.defaultTitle'),
      message: message || '',
      date: Number.isNaN(date.getTime()) ? new Date() : date,
      read,
      type,
      transactionId:
        transactionId !== undefined && transactionId !== null
          ? String(transactionId)
          : undefined,
      important: Boolean(o['important']),
    };
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
    if (!notification.read) {
      notification.read = true;
      this.unreadCount = Math.max(0, this.unreadCount - 1);
    }
  }

  markAllAsRead(): void {
    this.notifications.forEach((n) => {
      n.read = true;
    });
    this.unreadCount = 0;
    this.applyFilter();
  }

  viewTransaction(transactionId?: string): void {
    if (transactionId) {
      this.router.navigate(['/transaction', transactionId]);
    }
  }

  getNotificationIcon(type: string): string {
    const icons: Record<string, string> = {
      'new-transaction': '📨',
      approval: '✅',
      reminder: '⏰',
      system: '⚙️',
    };
    return icons[type] ?? '📢';
  }

  getNotificationClass(type: string): string {
    const classes: Record<string, string> = {
      'new-transaction': 'notification-new',
      approval: 'notification-success',
      reminder: 'notification-warning',
      system: 'notification-info',
    };
    return classes[type] ?? 'notification-default';
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
    if (confirm(this.i18n.instant('notifications.deleteConfirm'))) {
      const index = this.notifications.findIndex((n) => n.id === id);
      if (index !== -1) {
        if (!this.notifications[index].read) {
          this.unreadCount--;
        }
        this.notifications.splice(index, 1);
        this.applyFilter();
      }
    }
  }
}
