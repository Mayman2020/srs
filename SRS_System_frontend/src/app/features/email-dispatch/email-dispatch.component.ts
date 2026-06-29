import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NotificationDispatchApiService } from '../../core/api/notification-dispatch-api.service';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';

@Component({
  selector: 'app-email-dispatch',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, ErpPageShellComponent],
  templateUrl: './email-dispatch.component.html',
  styleUrl: './email-dispatch.component.scss'
})
export class EmailDispatchComponent {
  private readonly api = inject(NotificationDispatchApiService);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    to: ['', [Validators.required, Validators.email]],
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    body: ['', [Validators.required, Validators.maxLength(5000)]]
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
    this.api.dispatchEmail({ to: v.to.trim(), subject: v.subject.trim(), body: v.body.trim() }).subscribe({
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
