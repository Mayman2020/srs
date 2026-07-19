import { Component, OnInit, AfterViewInit, Inject, PLATFORM_ID, OnDestroy } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { AuthApiService } from '../../../core/api/auth-api.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { MfaOtpDialogComponent } from '../../../shared/dialogs/mfa-otp-dialog.component';
import { LookupLabelsService } from '../../../core/lookup/lookup-labels.service';
import { NotificationService } from '../../../core/services/notification.service';
import { catchError, of } from 'rxjs';
import { TextInputDialogComponent, TextInputDialogData } from '../../../shared/dialogs/text-input-dialog.component';
import { take } from 'rxjs/operators';
import { AuthTokenService } from '../../../core/auth/auth-token.service';

declare var particlesJS: unknown;

function isMfaRequired(err: HttpErrorResponse): boolean {
  if (err.status !== 403) {
    return false;
  }
  const body = err.error;
  if (body === 'MFA_REQUIRED') {
    return true;
  }
  return typeof body === 'string' && body.trim() === 'MFA_REQUIRED';
}

function isAccountLocked(err: HttpErrorResponse): boolean {
  if (err.status !== 403) {
    return false;
  }
  const body = err.error;
  if (body === 'ACCOUNT_LOCKED') {
    return true;
  }
  return typeof body === 'string' && body.trim() === 'ACCOUNT_LOCKED';
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, MatDialogModule]
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  form!: FormGroup;

  submitting = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authApi: AuthApiService,
    private i18n: I18nService,
    private lookupLabels: LookupLabelsService,
    private dialog: MatDialog,
    private notification: NotificationService,
    private tokens: AuthTokenService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnDestroy(): void {
    if (isPlatformBrowser(this.platformId)) {
      document.body.classList.remove('login-page');
    }
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
    if (isPlatformBrowser(this.platformId)) {
      document.body.classList.add('login-page');
      // Public login defaults to Arabic; use ?lang=en for English (overrides prior localStorage).
      const params = new URLSearchParams(window.location.search);
      const lang = params.get('lang') === 'en' ? 'en' : 'ar';
      this.i18n.loadLang(lang).subscribe({ error: () => {} });
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initParticles(), 100);
    }
  }

  private initParticles(): void {
    if (typeof (window as unknown as { particlesJS?: unknown }).particlesJS === 'undefined') {
      return;
    }
    const particlesJSFn = (window as unknown as { particlesJS: (id: string, opts: object) => void })
      .particlesJS;
    particlesJSFn('particles-js', {
      particles: {
        number: { value: 70, density: { enable: true, value_area: 800 } },
        color: { value: '#FFD700' },
        shape: { type: 'circle' },
        opacity: { value: 0.8, random: true, anim: { enable: true, speed: 1, opacity_min: 0.5, sync: false } },
        size: { value: 6, random: true, anim: { enable: true, speed: 3, size_min: 2, sync: false } },
        line_linked: { enable: true, distance: 150, color: '#FFD700', opacity: 0.7, width: 2.5 },
        move: { enable: true, speed: 2.5, direction: 'none', random: true, straight: false, out_mode: 'out', bounce: false }
      },
      interactivity: {
        detect_on: 'canvas',
        events: { onhover: { enable: true, mode: 'grab' }, onclick: { enable: true, mode: 'push' }, resize: true },
        modes: {
          grab: { distance: 200, line_linked: { opacity: 1 } },
          push: { particles_nb: 8 }
        }
      },
      retina_detect: true
    });
  }

  login(): void {
    if (this.form.invalid || this.submitting) {
      this.showToast(
        this.i18n.instant('auth.validationRequired'),
        this.i18n.instant('auth.validationRequired'),
        'error'
      );
      return;
    }
    const { username, password } = this.form.value;
    this.submitting = true;
    this.authApi.login({ username, password }).subscribe({
      next: () => {
        this.submitting = false;
        this.onLoginSuccess();
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.submitting = false;
        if (isMfaRequired(err)) {
          const u = String(username ?? '').trim();
          const p = String(password ?? '');
          if (!u) {
            this.showToast(
              this.i18n.instant('auth.loginErrorTitle'),
              this.i18n.instant('auth.validationRequired'),
              'error'
            );
            return;
          }
          this.openMfaDialog(u, p);
          return;
        }
        if (isAccountLocked(err)) {
          this.showToast(
            this.i18n.instant('auth.loginErrorTitle'),
            this.i18n.instant('auth.accountLocked'),
            'error'
          );
          return;
        }
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.showToast(this.i18n.instant('auth.loginErrorTitle'), msg, 'error');
      }
    });
  }

  /** Material dialog: OTP entry, resend, verify (tokens applied inside {@link AuthApiService#mfaVerify}). */
  private openMfaDialog(username: string, password: string): void {
    this.dialog
      .open(MfaOtpDialogComponent, {
        width: 'min(440px, 94vw)',
        disableClose: false,
        autoFocus: 'first-tabbable',
        data: { username, password }
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.onLoginSuccess();
        }
      });
  }

  private onLoginSuccess(): void {
    this.showToast(
      this.i18n.instant('auth.loginSuccessTitle'),
      this.i18n.instant('auth.loginSuccessMessage'),
      'success'
    );
    this.lookupLabels.load().pipe(catchError(() => of(undefined))).subscribe();
    const target = this.tokens.mustChangePassword()
      ? ['/profile']
      : ['/dashboard'];
    setTimeout(
      () => this.router.navigate(target, { fragment: this.tokens.mustChangePassword() ? 'password' : undefined }),
      2200
    );
  }

  forgotPassword(): void {
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(440px, 94vw)',
      autoFocus: 'dialog',
      data: {
        dialogTitle: this.i18n.instant('auth.forgotPasswordTitle'),
        labelKey: 'admin.colUsername',
        confirmKey: 'common.apply',
        required: true,
        multiline: false,
      } satisfies TextInputDialogData
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe((username) => {
        const u = String(username ?? '').trim();
        if (!u) {
          return;
        }
        this.authApi.forgotPassword(u).subscribe({
          next: () => {
            this.showToast(
              this.i18n.instant('auth.forgotPasswordTitle'),
              this.i18n.instant('auth.forgotPasswordSent'),
              'success'
            );
          },
          error: () => {
            this.showToast(
              this.i18n.instant('auth.forgotPasswordTitle'),
              this.i18n.instant('auth.forgotPasswordSent'),
              'info'
            );
          }
        });
      });
  }

  private showToast(
    title: string,
    message: string,
    kind: 'success' | 'error' | 'info' = 'info'
  ): void {
    const content = title && title !== message ? `${title}: ${message}` : message;
    if (kind === 'success') {
      this.notification.successRaw(content);
      return;
    }
    if (kind === 'error') {
      this.notification.errorRaw(content);
      return;
    }
    this.notification.infoRaw(content);
  }
}
