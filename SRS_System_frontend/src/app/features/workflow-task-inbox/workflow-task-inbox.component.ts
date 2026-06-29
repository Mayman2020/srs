import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import {
  PlatformWorkflowApiService,
  WorkflowTaskInboxRowDto
} from '../../core/api/platform-workflow-api.service';
import { SlaTaskStatusApiService } from '../../core/api/sla-task-status-api.service';
import { SlaTaskStatusDto } from '../../core/api/api-types';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import {
  WorkflowQuickActionDialogComponent,
  WorkflowQuickActionDialogData
} from '../../shared/dialogs/workflow-quick-action-dialog.component';

@Component({
  selector: 'app-workflow-task-inbox',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe, ErpPageShellComponent, SrsDataTableComponent],
  templateUrl: './workflow-task-inbox.component.html',
  styleUrl: './workflow-task-inbox.component.scss'
})
export class WorkflowTaskInboxComponent implements OnInit {
  private readonly api = inject(PlatformWorkflowApiService);
  private readonly slaApi = inject(SlaTaskStatusApiService);
  private readonly dialog = inject(MatDialog);

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
    if (!this.showOnlyBreached) {
      return this.rows;
    }
    return this.rows.filter((r) => this.slaByTask.get(r.taskId)?.overdue);
  }

  toggleFilter(): void {
    this.showOnlyBreached = !this.showOnlyBreached;
  }

  slaFor(r: WorkflowTaskInboxRowDto): SlaTaskStatusDto | null {
    return this.slaByTask.get(r.taskId) ?? null;
  }

  openQuickAction(row: WorkflowTaskInboxRowDto): void {
    if (!row.correspondenceId) {
      return;
    }
    this.dialog
      .open(WorkflowQuickActionDialogComponent, {
        width: 'min(520px, 94vw)',
        data: {
          correspondenceId: row.correspondenceId,
          referenceNumber: row.correspondenceReferenceNumber,
          subject: row.correspondenceTitle,
          routingFromDepartmentId: row.currentDepartmentId ?? null
        } satisfies WorkflowQuickActionDialogData
      })
      .afterClosed()
      .subscribe((changed) => {
        if (changed) {
          this.load();
        }
      });
  }

  humanizeSeconds(seconds: number): string {
    if (seconds == null || seconds < 0) {
      return '';
    }
    if (seconds < 60) {
      return `${seconds}s`;
    }
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      const remainMins = minutes % 60;
      return remainMins > 0 ? `${hours}h ${remainMins}m` : `${hours}h`;
    }
    const days = Math.floor(hours / 24);
    const remainHours = hours % 24;
    return remainHours > 0 ? `${days}d ${remainHours}h` : `${days}d`;
  }

  private loadSlaStatuses(): void {
    if (this.rows.length === 0) {
      return;
    }
    const calls = this.rows
      .filter((r) => !!r.taskId)
      .map((r) => this.slaApi.getStatus(r.taskId).pipe(catchError(() => of(null))));
    if (calls.length === 0) {
      return;
    }
    forkJoin(calls).subscribe((results) => {
      results.forEach((status) => {
        if (status?.taskId) {
          this.slaByTask.set(status.taskId, status);
        }
      });
    });
  }
}
