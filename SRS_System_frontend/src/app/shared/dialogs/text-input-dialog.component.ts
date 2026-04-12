import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';

export interface TextInputDialogData {
  /** Use either {@link titleKey} or {@link dialogTitle}. */
  titleKey?: string;
  /** Raw title (e.g. workflow action name from API). */
  dialogTitle?: string;
  labelKey: string;
  hintKey?: string;
  confirmKey: string;
  required: boolean;
  multiline: boolean;
  initialValue?: string;
}

@Component({
  selector: 'app-text-input-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslatePipe,
    ErpDialogComponent,
  ],
  template: `
    <app-erp-dialog
      [titleKey]="data.titleKey ?? 'transactionDetails.workflowTitle'"
      [titleText]="data.dialogTitle"
      icon="edit_note"
    >
      <p class="hint" *ngIf="data.hintKey">{{ data.hintKey | t }}</p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>{{ data.labelKey | t }}</mat-label>
        <textarea
          *ngIf="data.multiline"
          matInput
          rows="4"
          [(ngModel)]="value"></textarea>
        <input *ngIf="!data.multiline" matInput [(ngModel)]="value" />
      </mat-form-field>
      <div erpDialogActions>
        <button mat-button type="button" class="dialog-cancel-btn" (click)="cancel()">
          {{ 'common.close' | t }}
        </button>
        <button mat-flat-button type="button" class="dialog-confirm-btn" (click)="ok()">
          {{ data.confirmKey | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .full-width {
        width: 100%;
      }
      .hint {
        font-size: 14px;
        color: var(--muted, #64748b);
        margin: 0 0 8px;
      }
    `,
  ],
})
export class TextInputDialogComponent {
  value = '';

  constructor(
    private ref: MatDialogRef<TextInputDialogComponent, string | undefined>,
    @Inject(MAT_DIALOG_DATA) public data: TextInputDialogData
  ) {
    this.value = data.initialValue ?? '';
  }

  cancel(): void {
    this.ref.close(undefined);
  }

  ok(): void {
    const v = this.value.trim();
    if (this.data.required && !v) {
      return;
    }
    this.ref.close(v);
  }
}
