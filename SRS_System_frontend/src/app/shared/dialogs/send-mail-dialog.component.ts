import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationDispatchApiService } from '../../core/api/notification-dispatch-api.service';

export interface SendMailDialogData {
  defaultSubject: string;
  defaultBody: string;
}

@Component({
  selector: 'app-send-mail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    TranslatePipe,
  ],
  template: `
    <h2 mat-dialog-title>{{ 'transactionDetails.sendMailTitle' | t }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ 'transactionDetails.sendMailTo' | t }}</mat-label>
          <input matInput type="email" formControlName="to" autocomplete="email" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ 'transactionDetails.sendMailSubject' | t }}</mat-label>
          <input matInput formControlName="subject" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ 'transactionDetails.sendMailBody' | t }}</mat-label>
          <textarea matInput rows="6" formControlName="body"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="ref.close(false)" [disabled]="sending">
        {{ 'common.close' | t }}
      </button>
      <button
        mat-flat-button
        color="primary"
        type="button"
        (click)="submit()"
        [disabled]="form.invalid || sending">
        <span *ngIf="!sending">{{ 'transactionDetails.sendMailSubmit' | t }}</span>
        <span *ngIf="sending">{{ 'common.loading' | t }}</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .form {
        display: flex;
        flex-direction: column;
        min-width: min(420px, 88vw);
        padding-top: 8px;
      }
      .full {
        width: 100%;
      }
    `,
  ],
})
export class SendMailDialogComponent {
  form: ReturnType<FormBuilder['group']>;
  sending = false;

  constructor(
    public ref: MatDialogRef<SendMailDialogComponent, boolean>,
    private fb: FormBuilder,
    private dispatchApi: NotificationDispatchApiService,
    private i18n: I18nService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) data: SendMailDialogData
  ) {
    this.form = this.fb.group({
      to: ['', [Validators.required, Validators.email]],
      subject: [data.defaultSubject, Validators.required],
      body: [data.defaultBody, Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid || this.sending) {
      return;
    }
    const v = this.form.getRawValue();
    this.sending = true;
    this.dispatchApi
      .dispatchEmail({
        to: v.to!.trim(),
        subject: v.subject!.trim(),
        body: v.body!.trim(),
      })
      .subscribe({
        next: () => {
          this.sending = false;
          this.ref.close(true);
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.sending = false;
          const msg = err.userMessage ?? this.i18n.instant('transactionDetails.sendMailError');
          this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 6000 });
        },
      });
  }
}
