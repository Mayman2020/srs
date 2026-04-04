import { Component, OnInit, AfterViewInit, Inject, PLATFORM_ID, OnDestroy } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthApiService } from '../../../core/api/auth-api.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

declare var particlesJS: unknown;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe]
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  form!: FormGroup;

  toast = {
    show: false,
    title: '',
    message: ''
  };

  submitting = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authApi: AuthApiService,
    private i18n: I18nService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnDestroy(): void {
    document.body.classList.remove('login-page');
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
    document.body.classList.add('login-page');
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
        this.i18n.instant('auth.validationRequired')
      );
      return;
    }
    const { username, password } = this.form.value;
    this.submitting = true;
    this.authApi.login({ username, password }).subscribe({
      next: () => {
        this.submitting = false;
        this.showToast(
          this.i18n.instant('auth.loginSuccessTitle'),
          this.i18n.instant('auth.loginSuccessMessage')
        );
        setTimeout(() => this.router.navigate(['/dashboard']), 600);
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.submitting = false;
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.showToast(this.i18n.instant('errors.generic'), msg);
      }
    });
  }

  forgotPassword(): void {
    this.showToast(
      this.i18n.instant('auth.forgotPasswordTitle'),
      this.i18n.instant('auth.forgotPasswordMessage')
    );
  }

  private showToast(title: string, message: string): void {
    this.toast = { show: true, title, message };
    setTimeout(() => {
      this.toast.show = false;
    }, 4000);
  }
}
