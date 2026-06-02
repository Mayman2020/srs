import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NotificationDispatchApiService } from '../../core/api/notification-dispatch-api.service';

/**
 * Outbound SMS dispatch panel for system administrators / correspondence managers with
 * {@code NOTIFICATION_DISPATCH}. Reads the recipient phone in E.164 form and a message body.
 * Backend handles vendor delivery; this view only reports success / failure.
 */
@Component({
  selector: 'app-sms-dispatch',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  template: `
    <section class="sms-dispatch">
      <header class="sms-dispatch__header">
        <h1>{{ 'smsDispatch.pageTitle' | t }}</h1>
        <p class="sms-dispatch__subtitle">{{ 'smsDispatch.pageSubtitle' | t }}</p>
      </header>

      <form class="sms-dispatch__form" [formGroup]="form" (ngSubmit)="submit()">
        <label class="sms-dispatch__field">
          <span>{{ 'smsDispatch.phone' | t }}</span>
          <input type="tel" formControlName="phoneE164" placeholder="+9665XXXXXXXX"/>
          <small class="sms-dispatch__hint">{{ 'smsDispatch.phoneHint' | t }}</small>
        </label>
        <label class="sms-dispatch__field">
          <span>{{ 'smsDispatch.message' | t }}</span>
          <textarea formControlName="message" rows="4" maxlength="500"></textarea>
          <small class="sms-dispatch__hint">
            {{ form.controls.message.value.length || 0 }} / 500
          </small>
        </label>
        <div class="sms-dispatch__actions">
          <button type="submit" class="btn btn-primary" [disabled]="form.invalid || sending">
            {{ (sending ? 'common.sending' : 'smsDispatch.send') | t }}
          </button>
        </div>

        <p class="sms-dispatch__success" *ngIf="lastResultKey === 'sent'">{{ 'smsDispatch.successMessage' | t }}</p>
        <p class="sms-dispatch__error" *ngIf="lastResultKey === 'failed'">{{ 'smsDispatch.errorMessage' | t }}</p>
      </form>
    </section>
  `,
  styles: [
    `
      .sms-dispatch { max-width: 560px; padding: 1.5rem; }
      .sms-dispatch__subtitle { color: var(--text-muted, #6b7280); margin: 0.25rem 0 0; }
      .sms-dispatch__form { display: flex; flex-direction: column; gap: 1rem; margin-top: 1.5rem; }
      .sms-dispatch__field { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.9rem; }
      .sms-dispatch__field input, .sms-dispatch__field textarea {
        padding: 0.5rem 0.6rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db);
        font-family: inherit; font-size: 1rem;
      }
      .sms-dispatch__hint { color: var(--text-muted, #6b7280); font-size: 0.8rem; }
      .sms-dispatch__actions { display: flex; gap: 0.5rem; }
      .sms-dispatch__actions .btn { padding: 0.5rem 1.25rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db); cursor: pointer; background: var(--surface, #fff); }
      .sms-dispatch__actions .btn-primary { background: var(--primary, #0f766e); color: #fff; border-color: transparent; }
      .sms-dispatch__success { color: var(--success, #15803d); }
      .sms-dispatch__error { color: var(--danger, #b91c1c); }
    `
  ]
})
export class SmsDispatchComponent {
  private readonly api = inject(NotificationDispatchApiService);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    phoneE164: ['', [Validators.required, Validators.pattern(/^\+?[1-9]\d{6,14}$/)]],
    message: ['', [Validators.required, Validators.maxLength(500)]]
  });

  sending = false;
  lastResultKey: 'sent' | 'failed' | null = null;

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.sending = true;
    this.lastResultKey = null;
    const v = this.form.getRawValue();
    this.api.dispatchSms({ phoneE164: v.phoneE164.trim(), message: v.message.trim() }).subscribe({
      next: () => {
        this.lastResultKey = 'sent';
        this.sending = false;
        this.form.reset();
      },
      error: () => {
        this.lastResultKey = 'failed';
        this.sending = false;
      }
    });
  }
}
