import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { UserListDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';

export interface WorkflowReferDialogData {
  dialogTitle?: string;
}

export interface WorkflowReferDialogResult {
  targetUserId: string;
}

@Component({
  selector: 'app-workflow-refer-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, TranslatePipe, ErpDialogComponent],
  template: `
    <app-erp-dialog
      [titleKey]="data.dialogTitle ? '' : 'transactionDetails.workflowReferUserPrompt'"
      [titleText]="data.dialogTitle"
      icon="person_add"
    >
      <label class="field">
        <span>{{ 'transactionDetails.workflowReferUserPrompt' | t }}</span>
        <select class="custom-input" [(ngModel)]="targetUserId">
          <option value="">{{ 'common.select' | t }}</option>
          <option *ngFor="let u of users" [value]="u.id">{{ userLabel(u) }}</option>
        </select>
      </label>
      <div erpDialogActions>
        <button mat-button type="button" (click)="cancel()">{{ 'common.close' | t }}</button>
        <button mat-flat-button type="button" color="primary" (click)="confirm()" [disabled]="!targetUserId">
          {{ 'common.apply' | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        font-size: 0.9rem;
      }
      .custom-input {
        padding: 0.45rem 0.6rem;
        border-radius: 6px;
        border: 1px solid var(--border, #d1d5db);
        font: inherit;
      }
    `
  ]
})
export class WorkflowReferDialogComponent implements OnInit {
  private readonly usersApi = inject(UserDirectoryApiService);
  private readonly i18n = inject(I18nService);

  users: UserListDto[] = [];
  targetUserId = '';

  constructor(
    private readonly dialogRef: MatDialogRef<WorkflowReferDialogComponent, WorkflowReferDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: WorkflowReferDialogData
  ) {}

  ngOnInit(): void {
    this.usersApi.list(0, 500).subscribe({
      next: (page) => (this.users = page?.content ?? []),
      error: () => (this.users = [])
    });
  }

  userLabel(u: UserListDto): string {
    const name = this.i18n.currentLang() === 'en' ? u.fullNameEn : u.fullNameAr;
    return `${name} (${u.username})`;
  }

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (!this.targetUserId.trim()) {
      return;
    }
    this.dialogRef.close({ targetUserId: this.targetUserId.trim() });
  }
}
