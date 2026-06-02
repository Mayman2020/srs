import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { AuditApiService, AuditEventRecord } from '../../core/api/audit-api.service';

/**
 * Audit-event viewer for system administrators / auditors. Loads the most recent N audit
 * events with optional filters (actor, action code, time range). No client-side
 * pagination — the limit field bounds the result size on the server.
 *
 * Requires {@code ADMIN_AUDIT_VIEW}.
 */
@Component({
  selector: 'app-audit-events',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  template: `
    <section class="audit">
      <header class="audit__header">
        <h1>{{ 'audit.pageTitle' | t }}</h1>
        <p class="audit__subtitle">{{ 'audit.pageSubtitle' | t }}</p>
      </header>

      <form class="audit__filters" (submit)="$event.preventDefault(); load()">
        <label>
          <span>{{ 'audit.filter.actor' | t }}</span>
          <input type="text" [(ngModel)]="actor" name="actor"/>
        </label>
        <label>
          <span>{{ 'audit.filter.action' | t }}</span>
          <input type="text" [(ngModel)]="action" name="action"/>
        </label>
        <label>
          <span>{{ 'audit.filter.from' | t }}</span>
          <input type="datetime-local" [(ngModel)]="from" name="from"/>
        </label>
        <label>
          <span>{{ 'audit.filter.to' | t }}</span>
          <input type="datetime-local" [(ngModel)]="to" name="to"/>
        </label>
        <label>
          <span>{{ 'audit.filter.limit' | t }}</span>
          <input type="number" min="1" max="1000" [(ngModel)]="limit" name="limit"/>
        </label>
        <div class="audit__filter-actions">
          <button type="submit" class="btn btn-primary">{{ 'common.apply' | t }}</button>
          <button type="button" class="btn" (click)="reset()">{{ 'common.clear' | t }}</button>
        </div>
      </form>

      <p class="audit__error" *ngIf="errorKey">{{ errorKey | t }}</p>

      <table class="audit__table" *ngIf="!loading">
        <thead>
          <tr>
            <th>{{ 'audit.col.occurredAt' | t }}</th>
            <th>{{ 'audit.col.actor' | t }}</th>
            <th>{{ 'audit.col.action' | t }}</th>
            <th>{{ 'audit.col.resource' | t }}</th>
            <th>{{ 'audit.col.detail' | t }}</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let e of events">
            <td class="audit__when">{{ e.occurredAt }}</td>
            <td>{{ e.actorUserId }}</td>
            <td class="audit__action">{{ e.actionCode }}</td>
            <td>{{ e.resourceType }} {{ e.resourceId ? '#' + e.resourceId : '' }}</td>
            <td class="audit__detail"><pre>{{ e.detailJson }}</pre></td>
          </tr>
          <tr *ngIf="!events.length">
            <td colspan="5" class="audit__empty">{{ 'common.noResults' | t }}</td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="loading">{{ 'common.loading' | t }}</p>
    </section>
  `,
  styles: [
    `
      .audit { padding: 1.5rem; }
      .audit__header { margin-bottom: 1rem; }
      .audit__subtitle { color: var(--text-muted, #6b7280); margin: 0.25rem 0 0; }
      .audit__filters { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.75rem; margin-bottom: 1rem; }
      .audit__filters label { display: flex; flex-direction: column; font-size: 0.875rem; }
      .audit__filters input { padding: 0.4rem 0.5rem; border: 1px solid var(--border, #d1d5db); border-radius: 6px; }
      .audit__filter-actions { display: flex; align-items: flex-end; gap: 0.5rem; }
      .audit__filter-actions .btn { padding: 0.45rem 0.9rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db); background: var(--surface, #fff); cursor: pointer; }
      .audit__filter-actions .btn-primary { background: var(--primary, #0f766e); color: #fff; border-color: transparent; }
      .audit__error { color: var(--danger, #b91c1c); }
      .audit__table { width: 100%; border-collapse: collapse; }
      .audit__table th, .audit__table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid var(--border, #e5e7eb); vertical-align: top; text-align: start; }
      .audit__when { white-space: nowrap; font-variant-numeric: tabular-nums; }
      .audit__action { font-family: ui-monospace, monospace; font-size: 0.85rem; }
      .audit__detail pre { white-space: pre-wrap; word-break: break-word; max-width: 480px; margin: 0; font-size: 0.8rem; }
      .audit__empty { text-align: center; padding: 1.5rem; color: var(--text-muted, #6b7280); }
    `
  ]
})
export class AuditEventsComponent implements OnInit {
  private readonly api = inject(AuditApiService);

  actor = '';
  action = '';
  from = '';
  to = '';
  limit = 100;

  loading = true;
  errorKey: string | null = null;
  events: AuditEventRecord[] = [];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorKey = null;
    this.api
      .query({
        actor: this.actor || undefined,
        action: this.action || undefined,
        from: this.from ? new Date(this.from).toISOString() : undefined,
        to: this.to ? new Date(this.to).toISOString() : undefined,
        limit: this.limit || 100
      })
      .subscribe({
        next: (rows) => {
          this.events = rows ?? [];
          this.loading = false;
        },
        error: () => {
          this.events = [];
          this.errorKey = 'audit.loadFailed';
          this.loading = false;
        }
      });
  }

  reset(): void {
    this.actor = '';
    this.action = '';
    this.from = '';
    this.to = '';
    this.limit = 100;
    this.load();
  }
}
