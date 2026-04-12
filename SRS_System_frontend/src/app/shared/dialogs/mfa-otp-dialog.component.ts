import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuthApiService } from '../../core/api/auth-api.service';
import { ErpDialogComponent } from '../erp/erp-dialog.component';
import { NotificationService } from '../../core/services/notification.service';

export interface MfaOtpDialogData {
  username: string;
  password: string;
}

@Component({
  selector: 'app-mfa-otp-dialog',
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
    <app-erp-dialog [titleKey]="'auth.mfaStepTitle'" icon="verified_user">
      <div class="content">
        <p class="lead">{{ 'auth.mfaStepLead' | t }}</p>
        <div class="channels">
          <span class="label">{{ 'auth.mfaChannelLabel' | t }}</span>
          <label class="radio">
            <input
              type="radio"
              name="ch"
              value="EMAIL"
              [checked]="channel === 'EMAIL'"
              (change)="channel = 'EMAIL'" />
            {{ 'auth.mfaChannelEmail' | t }}
          </label>
          <label class="radio">
            <input
              type="radio"
              name="ch"
              value="SMS"
              [checked]="channel === 'SMS'"
              (change)="channel = 'SMS'" />
            {{ 'auth.mfaChannelSms' | t }}
          </label>
        </div>
        <button
          mat-stroked-button
          type="button"
          class="resend"
          (click)="sendCode()"
          [disabled]="sending">
          {{ 'auth.mfaSendCode' | t }}
        </button>
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ 'auth.mfaCodeLabel' | t }}</mat-label>
          <input
            matInput
            [(ngModel)]="code"
            name="otp"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="12"
            [attr.placeholder]="'auth.mfaCodePlaceholder' | t" />
        </mat-form-field>
      </div>
      <div erpDialogActions>
        <button mat-button type="button" class="dialog-cancel-btn" (click)="cancel()">
          {{ 'auth.mfaBack' | t }}
        </button>
        <button
          mat-flat-button
          type="button"
          class="dialog-confirm-btn"
          (click)="verify()"
          [disabled]="verifying">
          {{ 'auth.mfaVerifySubmit' | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .content {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-top: 0;
      }
      .lead {
        margin: 0;
        font-size: 0.9rem;
        color: var(--muted, #475569);
        line-height: 1.45;
      }
      .channels {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px 16px;
      }
      .label {
        font-weight: 600;
        font-size: 0.85rem;
        width: 100%;
      }
      .radio {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 0.9rem;
        cursor: pointer;
      }
      .resend {
        align-self: flex-start;
      }
      .full {
        width: 100%;
      }
    `,
  ],
})
export class MfaOtpDialogComponent implements OnInit {
  channel: 'EMAIL' | 'SMS' = 'EMAIL';
  code = '';
  sending = false;
  verifying = false;

  constructor(
    private ref: MatDialogRef<MfaOtpDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: MfaOtpDialogData,
    private authApi: AuthApiService,
    private i18n: I18nService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.sendCode();
  }

  cancel(): void {
    this.ref.close(false);
  }

  sendCode(): void {
    const u = this.data.username?.trim();
    if (!u || this.sending) {
      return;
    }
    this.sending = true;
    this.authApi.mfaChallenge(u, this.channel).subscribe({
      next: () => {
        this.sending = false;
        this.notification.successRaw(this.i18n.instant('auth.mfaCodeSent'));
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.sending = false;
        console.error('[MFA] challenge failed', err);
        const msg = err.userMessage ?? this.i18n.instant('auth.mfaSendFailed');
        this.notification.errorRaw(msg);
      },
    });
  }

  verify(): void {
    const u = this.data.username?.trim();
    const code = this.code.trim();
    if (!u || !code) {
      this.notification.warningRaw(this.i18n.instant('auth.mfaCodePlaceholder'));
      return;
    }
    this.verifying = true;
    this.authApi
      .mfaVerify({ username: u, password: this.data.password, code })
      .subscribe({
        next: () => {
          this.verifying = false;
          this.ref.close(true);
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.verifying = false;
          console.error('[MFA] verify failed', err);
          const msg = err.userMessage ?? this.i18n.instant('errors.generic');
          this.notification.errorRaw(msg);
        },
      });
  }
}
