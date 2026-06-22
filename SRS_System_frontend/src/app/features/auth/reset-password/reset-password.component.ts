import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthApiService } from '../../../core/api/auth-api.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { NotificationService } from '../../../core/services/notification.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  readonly form;
  submitting = false;
  done = false;
  token = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly authApi: AuthApiService,
    private readonly notification: NotificationService,
    private readonly i18n: I18nService
  ) {
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';
  }

  submit(): void {
    if (this.form.invalid || !this.token || this.submitting) {
      return;
    }
    const { newPassword, confirmPassword } = this.form.value;
    if (newPassword !== confirmPassword) {
      this.notification.warningRaw(this.i18n.instant('auth.resetPasswordMismatch'));
      return;
    }
    this.submitting = true;
    this.authApi.resetPassword(this.token, String(newPassword)).subscribe({
      next: () => {
        this.submitting = false;
        this.done = true;
        this.notification.success('auth.resetPasswordSuccess');
        setTimeout(() => this.router.navigate(['/login']), 1800);
      },
      error: () => {
        this.submitting = false;
        this.notification.error('auth.resetPasswordFailed');
      }
    });
  }
}
