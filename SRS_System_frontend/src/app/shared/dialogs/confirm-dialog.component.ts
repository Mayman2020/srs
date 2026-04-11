import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';

export interface ConfirmDialogData {
  titleKey: string;
  messageKey: string;
  confirmKey: string;
  cancelKey: string;
  /** Use warn color for destructive confirms */
  warn?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, TranslatePipe, ErpDialogComponent],
  template: `
    <app-erp-dialog [titleKey]="data.titleKey" icon="help_outline">
      <p class="msg">{{ data.messageKey | t }}</p>
      <div erpDialogActions>
        <button mat-button type="button" (click)="ref.close(false)">
          {{ data.cancelKey | t }}
        </button>
        <button
          mat-flat-button
          type="button"
          [color]="data.warn ? 'warn' : 'primary'"
          (click)="ref.close(true)">
          {{ data.confirmKey | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .msg {
        margin: 0;
        line-height: 1.5;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  constructor(
    public ref: MatDialogRef<ConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}
}
