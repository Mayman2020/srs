import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  NotificationOutboxAdminDto,
  NotificationOutboxApiService,
  NotificationOutboxPage,
  NotificationOutboxStatus
} from '../../core/api/notification-outbox-api.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

const STATUSES: NotificationOutboxStatus[] = ['PENDING', 'SENT', 'FAILED', 'DEAD', 'CANCELLED'];

/**
 * Slice 6 — notification outbox admin. Lists rows by status with paging, lets operators inspect
 * the last error / next attempt time, and exposes requeue / cancel actions for FAILED+DEAD
 * messages. Cancel requires a destructive confirm; requeue does not (it is reversible).
 */
@Component({
  selector: 'app-notification-outbox-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, SrsDatePipe],
  templateUrl: './notification-outbox-admin.component.html',
  styleUrl: './notification-outbox-admin.component.scss'
})
export class NotificationOutboxAdminComponent implements OnInit {
  private readonly api = inject(NotificationOutboxApiService);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);

  readonly statuses = STATUSES;
  readonly status = signal<NotificationOutboxStatus>('PENDING');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly page = signal<NotificationOutboxPage | null>(null);
  readonly rows = signal<NotificationOutboxAdminDto[]>([]);
  readonly selected = signal<NotificationOutboxAdminDto | null>(null);
  searchModel = '';

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.page(this.status(), this.pageIndex(), this.pageSize()).subscribe({
      next: (p) => {
        this.page.set(p);
        this.rows.set(this.applyClientSearch(p.content ?? []));
        this.loading.set(false);
      },
      error: () => {
        this.rows.set([]);
        this.page.set(null);
        this.error.set('notificationAdmin.outbox.loadFailed');
        this.loading.set(false);
      }
    });
  }

  applyClientSearch(rows: NotificationOutboxAdminDto[]): NotificationOutboxAdminDto[] {
    const q = (this.searchModel ?? '').trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter((r) =>
      [
        r.eventTypeCode,
        r.channelCode,
        r.recipientAddress ?? '',
        r.idempotencyKey ?? '',
        r.lastError ?? ''
      ]
        .map((s) => String(s).toLowerCase())
        .some((s) => s.includes(q))
    );
  }

  setStatus(s: NotificationOutboxStatus): void {
    this.status.set(s);
    this.pageIndex.set(0);
    this.refresh();
  }

  onSearch(_value: string): void {
    const all = this.page()?.content ?? [];
    this.rows.set(this.applyClientSearch(all));
  }

  prevPage(): void {
    if (this.pageIndex() > 0) {
      this.pageIndex.update((v) => v - 1);
      this.refresh();
    }
  }

  nextPage(): void {
    const p = this.page();
    if (p && !p.last) {
      this.pageIndex.update((v) => v + 1);
      this.refresh();
    }
  }

  select(row: NotificationOutboxAdminDto): void {
    this.selected.set(row);
  }

  closeDetails(): void {
    this.selected.set(null);
  }

  canRequeue(row: NotificationOutboxAdminDto): boolean {
    return row.status === 'FAILED' || row.status === 'DEAD' || row.status === 'CANCELLED';
  }

  canCancel(row: NotificationOutboxAdminDto): boolean {
    return row.status === 'PENDING' || row.status === 'FAILED' || row.status === 'DEAD';
  }

  requeue(row: NotificationOutboxAdminDto): void {
    this.api.requeue(row.id).subscribe({
      next: () => {
        this.toast.success('notificationAdmin.outbox.requeuedToast');
        this.refresh();
      },
      error: () => this.toast.error('notificationAdmin.outbox.requeueFailedToast')
    });
  }

  confirmCancel(row: NotificationOutboxAdminDto): void {
    this.dialog
      .openConfirm({
        titleKey: 'notificationAdmin.outbox.cancelConfirmTitle',
        messageKey: 'notificationAdmin.outbox.cancelConfirmMessage',
        params: { code: row.eventTypeCode + '/' + row.channelCode },
        confirmButton: { labelKey: 'common.confirm', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.cancel(row.id).subscribe({
          next: () => {
            this.toast.success('notificationAdmin.outbox.cancelledToast');
            this.refresh();
          },
          error: () => this.toast.error('notificationAdmin.outbox.cancelFailedToast')
        });
      });
  }

  statusBadge(s: string): string {
    switch (s) {
      case 'PENDING':
        return 'badge-pending';
      case 'SENT':
        return 'badge-sent';
      case 'FAILED':
        return 'badge-failed';
      case 'DEAD':
        return 'badge-dead';
      case 'CANCELLED':
        return 'badge-cancelled';
      default:
        return 'badge-default';
    }
  }
}
