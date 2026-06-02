import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import {
  PlatformWorkflowApiService,
  WorkflowTaskInboxRowDto
} from '../../core/api/platform-workflow-api.service';
import { SlaTaskStatusApiService } from '../../core/api/sla-task-status-api.service';
import { SlaTaskStatusDto } from '../../core/api/api-types';

/**
 * Workflow task inbox for the current user — shows every active Camunda user task where the
 * caller is the assignee, a candidate user, or in a candidate group. Each row links to the
 * corresponding correspondence workspace.
 *
 * Slice 3: enriches each row with a per-task SLA status fetched from
 * `/api/v1/sla/tasks/{taskId}/status`. The status drives the overdue badge, the countdown chip,
 * and the "show breached only" filter. Failures are logged but never block the inbox: if the
 * status endpoint is unavailable the table renders without SLA decorations.
 */
@Component({
  selector: 'app-workflow-task-inbox',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  template: `
    <section class="task-inbox">
      <header class="task-inbox__header">
        <h1>{{ 'workflowTasks.pageTitle' | t }}</h1>
        <p class="task-inbox__subtitle">{{ 'workflowTasks.pageSubtitle' | t }}</p>
        <label class="task-inbox__filter">
          <input type="checkbox" [checked]="showOnlyBreached" (change)="toggleFilter()" />
          <span>{{ 'workflowTasks.filterOnlyBreached' | t }}</span>
        </label>
        <button type="button" class="btn" (click)="load()" [disabled]="loading">
          {{ 'common.refresh' | t }}
        </button>
      </header>

      <p class="task-inbox__error" *ngIf="errorKey">{{ errorKey | t }}</p>

      <table class="task-inbox__table" *ngIf="!loading">
        <thead>
          <tr>
            <th>{{ 'workflowTasks.col.ref' | t }}</th>
            <th>{{ 'workflowTasks.col.title' | t }}</th>
            <th>{{ 'workflowTasks.col.task' | t }}</th>
            <th>{{ 'workflowTasks.col.level' | t }}</th>
            <th>{{ 'workflowTasks.col.status' | t }}</th>
            <th>{{ 'workflowTasks.col.priority' | t }}</th>
            <th>{{ 'workflowTasks.col.createdAt' | t }}</th>
            <th>{{ 'workflowTasks.col.sla' | t }}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr
            *ngFor="let r of visibleRows()"
            [class.task-inbox__row-delegated]="r.actingAsDelegate"
            [class.task-inbox__row-acting-manager]="r.actingAsManager"
            [class.task-inbox__row-overdue]="slaFor(r)?.overdue"
          >
            <td class="task-inbox__ref">{{ r.correspondenceReferenceNumber || '—' }}</td>
            <td class="task-inbox__title">
              {{ r.correspondenceTitle || '—' }}
              <span
                *ngIf="r.actingAsDelegate"
                class="task-inbox__badge task-inbox__badge--acting"
                [attr.title]="'workflowTasks.actingForTooltip' | t"
              >
                {{ 'workflowTasks.actingFor' | t }}
              </span>
              <span
                *ngIf="r.actingAsManager"
                class="task-inbox__badge task-inbox__badge--acting-manager"
                [attr.title]="'workflowTasks.actingManagerTooltip' | t"
              >
                {{ 'workflowTasks.actingManager' | t }}
              </span>
              <span
                *ngIf="slaFor(r) as s"
                [class.task-inbox__badge--overdue]="s.overdue"
                [class.task-inbox__badge--ontrack]="!s.overdue"
                class="task-inbox__badge"
              >
                {{ s.overdue ? ('workflowTasks.sla.overdueBy' | t) : ('workflowTasks.sla.remaining' | t) }}
                {{ humanizeSeconds(s.overdue ? s.secondsOverdue : s.secondsRemaining) }}
              </span>
            </td>
            <td>{{ r.taskName || r.taskDefinitionKey || '—' }}</td>
            <td>{{ r.currentLevelCode || '—' }}</td>
            <td>{{ r.correspondenceStatusCode || '—' }}</td>
            <td>{{ r.priorityCode || '—' }}</td>
            <td class="task-inbox__when">{{ r.createdAt }}</td>
            <td class="task-inbox__sla">
              <ng-container *ngIf="slaFor(r) as s; else noSla">
                <span class="task-inbox__sla-code">{{ s.slaPolicyCode || '—' }}</span>
                <span *ngIf="s.lastStepActionCode" class="task-inbox__sla-step">
                  · {{ ('sla.action.' + s.lastStepActionCode) | t }}
                </span>
              </ng-container>
              <ng-template #noSla>—</ng-template>
            </td>
            <td>
              <a
                *ngIf="r.correspondenceId"
                class="btn btn-link"
                [routerLink]="['/correspondence', r.correspondenceId]"
              >
                {{ 'common.open' | t }}
              </a>
            </td>
          </tr>
          <tr *ngIf="!visibleRows().length">
            <td colspan="9" class="task-inbox__empty">{{ 'common.noResults' | t }}</td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="loading">{{ 'common.loading' | t }}</p>
    </section>
  `,
  styles: [
    `
      .task-inbox { padding: 1.5rem; }
      .task-inbox__header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
      .task-inbox__header h1 { margin-inline-end: auto; }
      .task-inbox__subtitle { color: var(--text-muted, #6b7280); margin: 0; }
      .task-inbox__filter { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.88rem; color: #374151; }
      .task-inbox__error { color: var(--danger, #b91c1c); }
      .task-inbox__table { width: 100%; border-collapse: collapse; }
      .task-inbox__table th, .task-inbox__table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--border, #e5e7eb); text-align: start; vertical-align: top; }
      .task-inbox__ref { font-family: ui-monospace, monospace; white-space: nowrap; }
      .task-inbox__when { white-space: nowrap; font-variant-numeric: tabular-nums; }
      .task-inbox__empty { text-align: center; padding: 1.5rem; color: var(--text-muted, #6b7280); }
      .btn { padding: 0.35rem 0.8rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db); background: var(--surface, #fff); cursor: pointer; }
      .btn-link { color: var(--primary, #0f766e); text-decoration: none; }
      .task-inbox__badge { display: inline-block; margin-inline-start: 0.5rem; padding: 0.1rem 0.55rem; border-radius: 999px; font-size: 0.72rem; font-weight: 600; vertical-align: middle; }
      .task-inbox__badge--acting { background: rgba(217, 119, 6, 0.15); color: #b45309; border: 1px solid rgba(217, 119, 6, 0.35); }
      .task-inbox__badge--acting-manager { background: rgba(37, 99, 235, 0.12); color: #1d4ed8; border: 1px solid rgba(37, 99, 235, 0.35); }
      .task-inbox__row-delegated { background: rgba(217, 119, 6, 0.04); }
      .task-inbox__row-acting-manager { background: rgba(37, 99, 235, 0.04); }
      .task-inbox__badge--ontrack { background: rgba(16, 185, 129, 0.12); color: #047857; border: 1px solid rgba(16, 185, 129, 0.32); }
      .task-inbox__badge--overdue { background: rgba(220, 38, 38, 0.12); color: #b91c1c; border: 1px solid rgba(220, 38, 38, 0.4); }
      .task-inbox__row-overdue { background: rgba(220, 38, 38, 0.05); }
      .task-inbox__sla { font-size: 0.85rem; color: #4b5563; }
      .task-inbox__sla-code { font-family: ui-monospace, monospace; }
    `
  ]
})
export class WorkflowTaskInboxComponent implements OnInit {
  private readonly api = inject(PlatformWorkflowApiService);
  private readonly slaApi = inject(SlaTaskStatusApiService);
  private readonly router = inject(Router);

  rows: WorkflowTaskInboxRowDto[] = [];
  slaByTask = new Map<string, SlaTaskStatusDto>();
  showOnlyBreached = false;
  loading = true;
  errorKey: string | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorKey = null;
    this.slaByTask.clear();
    this.api.myInbox(200).subscribe({
      next: (rows) => {
        this.rows = rows ?? [];
        this.loading = false;
        this.loadSlaStatuses();
      },
      error: () => {
        this.rows = [];
        this.errorKey = 'workflowTasks.loadFailed';
        this.loading = false;
      }
    });
  }

  visibleRows(): WorkflowTaskInboxRowDto[] {
    if (!this.showOnlyBreached) return this.rows;
    return this.rows.filter((r) => this.slaByTask.get(r.taskId)?.overdue);
  }

  toggleFilter(): void {
    this.showOnlyBreached = !this.showOnlyBreached;
  }

  slaFor(r: WorkflowTaskInboxRowDto): SlaTaskStatusDto | null {
    return this.slaByTask.get(r.taskId) ?? null;
  }

  humanizeSeconds(seconds: number): string {
    if (seconds == null || seconds < 0) return '';
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      const remainMins = minutes % 60;
      return remainMins > 0 ? `${hours}h ${remainMins}m` : `${hours}h`;
    }
    const days = Math.floor(hours / 24);
    const remainHours = hours % 24;
    return remainHours > 0 ? `${days}d ${remainHours}h` : `${days}d`;
  }

  /**
   * Fetches SLA status for each visible task in parallel. We deliberately don't await any single
   * failure: the inbox is usable without SLA decorations and any one task's status failing
   * (e.g. completed concurrently) must not poison the whole table.
   */
  private loadSlaStatuses(): void {
    if (this.rows.length === 0) return;
    const calls = this.rows
      .filter((r) => !!r.taskId)
      .map((r) => this.slaApi.getStatus(r.taskId).pipe(catchError(() => of(null))));
    if (calls.length === 0) return;
    forkJoin(calls).subscribe((results) => {
      results.forEach((status) => {
        if (status && status.taskId) {
          this.slaByTask.set(status.taskId, status);
        }
      });
    });
  }
}
