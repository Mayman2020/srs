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
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationDispatchApiService } from '../../core/api/notification-dispatch-api.service';
import { ErpDialogComponent } from '../erp/erp-dialog.component';
import { NotificationService } from '../../core/services/notification.service';

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
    TranslatePipe,
    ErpDialogComponent,
  ],
  template: `
    <app-erp-dialog [titleKey]="'transactionDetails.sendMailTitle'" icon="mail" [wide]="true">
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
      <div erpDialogActions>
        <button
          mat-button
          type="button"
          class="dialog-cancel-btn"
          (click)="ref.close(false)"
          [disabled]="sending">
          {{ 'common.close' | t }}
        </button>
        <button
          mat-flat-button
          class="dialog-confirm-btn"
          type="button"
          (click)="submit()"
          [disabled]="form.invalid || sending">
          <span *ngIf="!sending">{{ 'transactionDetails.sendMailSubmit' | t }}</span>
          <span *ngIf="sending">{{ 'common.loading' | t }}</span>
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .form {
        display: flex;
        flex-direction: column;
        padding-top: 4px;
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
    private notification: NotificationService,
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
          this.notification.errorRaw(msg);
        },
      });
  }
}
