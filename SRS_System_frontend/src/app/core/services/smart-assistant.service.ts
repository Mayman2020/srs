import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { DashboardApiService } from '../api/dashboard-api.service';
import { NotificationApiService } from '../api/notification-api.service';
import { ReportsApiService } from '../api/reports-api.service';
import { UiFormatService } from '../i18n/ui-format.service';
import { I18nService } from '../i18n/i18n.service';
import { SmartAssistantAction, SmartAssistantReply } from '../models/smart-assistant.model';
import { TransactionService } from '../../services/transaction.service';

@Injectable({ providedIn: 'root' })
export class SmartAssistantService {
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly transactionService = inject(TransactionService);
  private readonly reportsApi = inject(ReportsApiService);
  private readonly notificationApi = inject(NotificationApiService);
  private readonly format = inject(UiFormatService);
  private readonly i18n = inject(I18nService);

  readonly suggestionKeys = [
    'chat.quick.pending',
    'chat.quick.overdue',
    'chat.quick.latest',
    'chat.quick.notifications'
  ] as const;

  readonly emptyStateActions: SmartAssistantAction[] = [
    { id: 'pending', label: this.i18n.instant('chat.quick.pending'), prompt: this.i18n.instant('chat.quick.pending') },
    { id: 'overdue', label: this.i18n.instant('chat.quick.overdue'), prompt: this.i18n.instant('chat.quick.overdue') },
    { id: 'latest', label: this.i18n.instant('chat.quick.latest'), prompt: this.i18n.instant('chat.quick.latest') },
    {
      id: 'transactions',
      label: this.i18n.instant('chat.actions.openTransactions'),
      route: '/transactions'
    }
  ];

  answer(query: string): Observable<SmartAssistantReply> {
    const normalized = query.trim().toLowerCase();

    if (!normalized) {
      return of({
        text: this.i18n.instant('chat.emptyPrompt'),
        actions: this.emptyStateActions
      });
    }

    if (this.matches(normalized, ['latest', 'recent', 'احدث', 'أحدث', 'آخر', 'اخير'])) {
      return this.transactionService.listPage({ size: 5 }).pipe(
        map((rows) => {
          if (!rows.length) {
            return {
              text: this.i18n.instant('chat.latestEmpty'),
              actions: [{ id: 'transactions', label: this.i18n.instant('chat.actions.openTransactions'), route: '/transactions' }]
            };
          }

          const lines = rows.map((row, index) =>
            `${this.format.formatNumber(index + 1, { useGrouping: false })}. ${row.referenceNumber ?? row.id} - ${row.subject}`
          );
          return {
            text: `${this.i18n.instant('chat.latestIntro')}\n${lines.join('\n')}`,
            actions: rows.slice(0, 3).map((row) => ({
              id: row.id,
              label: row.referenceNumber ?? row.id,
              route: `/transactions/${row.id}`
            }))
          };
        })
      );
    }

    if (this.matches(normalized, ['pending', 'in progress', 'open', 'قيد', 'معلقة', 'معلق', 'قيد التنفيذ'])) {
      return this.dashboardApi.getDashboard().pipe(
        map((dash) => ({
          text: this.i18n.instant('chat.pendingAnswer', {
            n: this.format.formatNumber(dash.kpiPipelineCount)
          }),
          actions: [
            { id: 'transactions', label: this.i18n.instant('chat.actions.openTransactions'), route: '/transactions' },
            { id: 'reports', label: this.i18n.instant('chat.actions.openReports'), route: '/reports' }
          ]
        }))
      );
    }

    if (this.matches(normalized, ['late', 'overdue', 'delayed', 'متأخرة', 'متاخر', 'متأخر', 'متاخرة'])) {
      return forkJoin({
        dash: this.dashboardApi.getDashboard(),
        heatmap: this.reportsApi.departmentSlaHeatmap()
      }).pipe(
        map(({ dash, heatmap }) => {
          const top = [...(heatmap ?? [])].sort((a, b) => b.overdueOpen - a.overdueOpen)[0];
          const dept = top
            ? this.i18n.currentLang() === 'ar'
              ? top.nameAr
              : top.nameEn
            : '';
          const suffix = top && top.overdueOpen > 0
            ? ` ${this.i18n.instant('chat.overdueDepartment', {
                dept,
                n: this.format.formatNumber(top.overdueOpen)
              })}`
            : '';
          return {
            text: `${this.i18n.instant('chat.overdueAnswer', {
              n: this.format.formatNumber(dash.overdueCount)
            })}${suffix}`,
            actions: [
              { id: 'reports', label: this.i18n.instant('chat.actions.openReports'), route: '/reports' },
              { id: 'dashboard', label: this.i18n.instant('chat.actions.openDashboard'), route: '/dashboard' }
            ]
          };
        })
      );
    }

    if (this.matches(normalized, ['notification', 'notifications', 'اشعار', 'إشعار', 'اشعارات', 'إشعارات'])) {
      return this.notificationApi.list(0, 5).pipe(
        map((page) => {
          const rows = page.content ?? [];
          if (!rows.length) {
            return {
              text: this.i18n.instant('chat.notificationsEmpty'),
              actions: [{ id: 'notifications', label: this.i18n.instant('chat.actions.openNotifications'), route: '/notifications' }]
            };
          }

          return {
            text: this.i18n.instant('chat.notificationsAnswer', {
              n: this.format.formatNumber(rows.length)
            }),
            actions: [{ id: 'notifications', label: this.i18n.instant('chat.actions.openNotifications'), route: '/notifications' }]
          };
        })
      );
    }

    if (this.matches(normalized, ['count', 'total', 'عدد', 'اجمالي', 'إجمالي'])) {
      return this.dashboardApi.getDashboard().pipe(
        map((dash) => ({
          text: this.i18n.instant('chat.totalAnswer', {
            total: this.format.formatNumber(dash.totalCorrespondences),
            done: this.format.formatNumber(dash.kpiSlaDoneCount),
            inbound: this.format.formatNumber(dash.kpiInboxCount)
          }),
          actions: [
            { id: 'dashboard', label: this.i18n.instant('chat.actions.openDashboard'), route: '/dashboard' },
            { id: 'transactions', label: this.i18n.instant('chat.actions.openTransactions'), route: '/transactions' }
          ]
        }))
      );
    }

    const refMatch = query.match(/[0-9a-f]{8}-[0-9a-f-]{27,}|[A-Za-z]*\d+[/-]\d+/i);
    if (refMatch) {
      return this.transactionService.listPage({ size: 100 }).pipe(
        map((rows) => {
          const found = rows.find(
            (row) =>
              row.id.toLowerCase() === refMatch[0].toLowerCase() ||
              row.referenceNumber?.toLowerCase() === refMatch[0].toLowerCase()
          );
          if (!found) {
            return {
              text: this.i18n.instant('chat.notFoundLive'),
              actions: [{ id: 'transactions', label: this.i18n.instant('chat.actions.openTransactions'), route: '/transactions' }]
            };
          }

          return {
            text: this.i18n.instant('chat.referenceAnswer', {
              ref: found.referenceNumber ?? found.id,
              subject: found.subject,
              status: found.status
            }),
            actions: [{ id: found.id, label: this.i18n.instant('chat.actions.openReference'), route: `/transactions/${found.id}` }]
          };
        })
      );
    }

    return this.dashboardApi.getDashboard().pipe(
      switchMap((dash) =>
        this.transactionService.listPage({ size: 3 }).pipe(
          map((latest) => ({
            text: this.i18n.instant('chat.fallbackAnswer', {
              total: this.format.formatNumber(dash.totalCorrespondences),
              overdue: this.format.formatNumber(dash.overdueCount)
            }),
            actions: [
              { id: 'pending', label: this.i18n.instant('chat.quick.pending'), prompt: this.i18n.instant('chat.quick.pending') },
              { id: 'latest', label: this.i18n.instant('chat.quick.latest'), prompt: this.i18n.instant('chat.quick.latest') },
              ...(latest[0]
                ? [{ id: latest[0].id, label: latest[0].referenceNumber ?? latest[0].id, route: `/transactions/${latest[0].id}` }]
                : [])
            ]
          }))
        )
      )
    );
  }

  private matches(query: string, candidates: string[]): boolean {
    return candidates.some((candidate) => query.includes(candidate));
  }
}
