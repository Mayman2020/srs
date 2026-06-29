import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { HttpErrorResponse } from '@angular/common/http';
import { take } from 'rxjs/operators';
import { CorrespondenceApiService } from '../../core/api/correspondence-api.service';
import { WorkflowActionAvailableDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';
import { TextInputDialogComponent, TextInputDialogData } from './text-input-dialog.component';
import {
  WorkflowForwardDialogComponent,
  WorkflowForwardDialogData,
  WorkflowForwardDialogResult
} from './workflow-forward-dialog.component';
import {
  WorkflowReferDialogComponent,
  WorkflowReferDialogData,
  WorkflowReferDialogResult
} from './workflow-refer-dialog.component';
import { NotificationService } from '../../core/services/notification.service';

export interface WorkflowQuickActionDialogData {
  correspondenceId: string;
  referenceNumber?: string | null;
  subject?: string | null;
  /** Origin department for routing preview on forward actions. */
  routingFromDepartmentId?: number | null;
}

@Component({
  selector: 'app-workflow-quick-action-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, TranslatePipe, ErpDialogComponent],
  template: `
    <app-erp-dialog [titleKey]="'workflowQuickAction.title'" icon="bolt">
      <p class="meta" *ngIf="data.referenceNumber || data.subject">
        <strong *ngIf="data.referenceNumber">{{ data.referenceNumber }}</strong>
        <span *ngIf="data.subject"> — {{ data.subject }}</span>
      </p>
      <p *ngIf="loading" class="muted">{{ 'common.loading' | t }}</p>
      <p *ngIf="errorKey" class="error">{{ errorKey | t }}</p>
      <div class="actions" *ngIf="!loading && actions.length">
        <button
          type="button"
          class="wa-btn"
          *ngFor="let a of actions"
          [class.wa-btn--primary]="(a.uiVariant ?? '').toLowerCase() === 'primary'"
          (click)="onAction(a)"
        >
          {{ actionLabel(a) }}
        </button>
      </div>
      <p *ngIf="!loading && !actions.length && !errorKey" class="muted">
        {{ 'workflowQuickAction.empty' | t }}
      </p>
      <div erpDialogActions>
        <button mat-button type="button" (click)="close()">{{ 'common.close' | t }}</button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .meta {
        margin: 0 0 1rem;
        font-size: 0.9rem;
      }
      .muted {
        color: var(--text-muted, #6b7280);
      }
      .error {
        color: var(--danger, #dc2626);
      }
      .actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }
      .wa-btn {
        border: 1px solid var(--border, #d1d5db);
        border-radius: 6px;
        padding: 0.45rem 0.85rem;
        background: var(--surface, #fff);
        cursor: pointer;
        font: inherit;
      }
      .wa-btn--primary {
        background: var(--primary, #0f766e);
        color: #fff;
        border-color: transparent;
      }
    `
  ]
})
export class WorkflowQuickActionDialogComponent implements OnInit {
  private readonly api = inject(CorrespondenceApiService);
  private readonly dialog = inject(MatDialog);
  private readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  actions: WorkflowActionAvailableDto[] = [];
  loading = true;
  errorKey: string | null = null;

  constructor(
    private readonly dialogRef: MatDialogRef<WorkflowQuickActionDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: WorkflowQuickActionDialogData
  ) {}

  ngOnInit(): void {
    this.api.listWorkflowActions(this.data.correspondenceId).subscribe({
      next: (rows) => {
        this.actions = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.actions = [];
        this.loading = false;
        this.errorKey = 'workflowQuickAction.loadFailed';
      }
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  actionLabel(a: WorkflowActionAvailableDto): string {
    return this.i18n.currentLang() === 'en' ? a.nameEn : a.nameAr;
  }

  onAction(a: WorkflowActionAvailableDto): void {
    if (a.requiresTargetUser) {
      this.promptRefer(a);
      return;
    }
    if (a.requiresTargetDepartment) {
      this.promptForward(a);
      return;
    }
    if (a.requiresComment) {
      this.promptComment(a);
      return;
    }
    this.run(a.code);
  }

  private promptComment(a: WorkflowActionAvailableDto, targetUserId?: string, targetDepartmentId?: number): void {
    this.dialog
      .open(TextInputDialogComponent, {
        width: 'min(480px, 94vw)',
        data: {
          dialogTitle: this.actionLabel(a),
          labelKey: 'transactionDetails.workflowCommentPrompt',
          confirmKey: 'common.apply',
          required: true,
          multiline: true
        } satisfies TextInputDialogData
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((comment) => {
        if (!String(comment ?? '').trim()) {
          return;
        }
        this.run(a.code, String(comment).trim(), targetUserId, targetDepartmentId);
      });
  }

  private promptRefer(a: WorkflowActionAvailableDto): void {
    this.dialog
      .open(WorkflowReferDialogComponent, {
        width: 'min(520px, 94vw)',
        data: { dialogTitle: this.actionLabel(a) } satisfies WorkflowReferDialogData
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((result: WorkflowReferDialogResult | undefined) => {
        if (!result?.targetUserId) {
          return;
        }
        if (a.requiresComment) {
          this.promptComment(a, result.targetUserId);
          return;
        }
        this.run(a.code, null, result.targetUserId);
      });
  }

  private promptForward(a: WorkflowActionAvailableDto): void {
    this.dialog
      .open(WorkflowForwardDialogComponent, {
        width: 'min(560px, 94vw)',
        data: {
          dialogTitle: this.actionLabel(a),
          routingFromDepartmentId: this.data.routingFromDepartmentId ?? null
        } satisfies WorkflowForwardDialogData
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((result: WorkflowForwardDialogResult | undefined) => {
        if (!result?.targetDepartmentId) {
          return;
        }
        if (a.requiresComment) {
          this.promptComment(a, undefined, result.targetDepartmentId);
          return;
        }
        this.run(a.code, null, undefined, result.targetDepartmentId);
      });
  }

  private run(
    action: string,
    comment?: string | null,
    targetUserId?: string | null,
    targetDepartmentId?: number | null
  ): void {
    this.api
      .workflowAction(this.data.correspondenceId, {
        action,
        comment,
        targetUserId,
        targetDepartmentId
      })
      .subscribe({
        next: () => {
          this.notification.successRaw(this.i18n.instant('workflowQuickAction.success'));
          this.dialogRef.close(true);
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.notification.errorRaw(err.userMessage ?? this.i18n.instant('errors.generic'));
        }
      });
  }
}
