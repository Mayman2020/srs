import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

export interface TextInputDialogData {
  titleKey: string;
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
  ],
  template: `
    <h2 mat-dialog-title>{{ data.titleKey | t }}</h2>
    <mat-dialog-content>
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
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancel()">{{ 'common.close' | t }}</button>
      <button mat-flat-button color="primary" type="button" (click)="ok()">
        {{ data.confirmKey | t }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .full-width {
        width: 100%;
      }
      .hint {
        font-size: 14px;
        color: #64748b;
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
