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
import { catchError, of, timeout } from 'rxjs';

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

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, MatDialogModule]
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  form!: FormGroup;

  toast: { show: boolean; title: string; message: string; kind: 'success' | 'error' | 'info' } = {
    show: false,
    title: '',
    message: '',
    kind: 'info'
  };

  submitting = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authApi: AuthApiService,
    private i18n: I18nService,
    private lookupLabels: LookupLabelsService,
    private dialog: MatDialog,
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
      this.form.markAllAsTouched();
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
        const msg =
          err.status === 401 || err.status === 403
            ? this.i18n.instant('errors.badCredentials')
            : err.userMessage ?? this.i18n.instant('errors.generic');
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
    
    // Load lookup labels in background with timeout to prevent hanging
    this.lookupLabels
      .load()
      .pipe(
        timeout(5000), // 5 second timeout
        catchError(() => of(undefined))
      )
      .subscribe();
    
    // Navigate quickly - toast will still be visible during navigation
    setTimeout(() => this.router.navigate(['/dashboard']), 800);
  }

  forgotPassword(): void {
    this.showToast(
      this.i18n.instant('auth.forgotPasswordTitle'),
      this.i18n.instant('auth.forgotPasswordMessage'),
      'info'
    );
  }

  private showToast(
    title: string,
    message: string,
    kind: 'success' | 'error' | 'info' = 'info'
  ): void {
    this.toast = { show: true, title, message, kind };
    const ms = kind === 'error' ? 6000 : 4000;
    setTimeout(() => {
      this.toast.show = false;
    }, ms);
  }
}
