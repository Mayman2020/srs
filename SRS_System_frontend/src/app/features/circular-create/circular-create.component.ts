import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { PlatformCircularApiService } from '../../core/api/platform-circular-api.service';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { UserListDto } from '../../core/api/api-types';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { NotificationService } from '../../core/services/notification.service';
import { I18nService } from '../../core/i18n/i18n.service';

/**
 * Create / broadcast circulars to specific recipients or to all platform users.
 *
 * Requires {@code CORRESPONDENCE_CREATE}; broadcast also requires {@code NOTIFICATION_DISPATCH}.
 * Backend overrides {@code createdBy} from the JWT, but we still send the local user id so the
 * payload validator passes.
 */
@Component({
  selector: 'app-circular-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  template: `
    <section class="circular-create">
      <header>
        <h1>{{ 'circularCreate.pageTitle' | t }}</h1>
        <p class="muted">{{ 'circularCreate.pageSubtitle' | t }}</p>
      </header>

      <form [formGroup]="form" (ngSubmit)="submit()" class="circular-create__form">
        <label class="field">
          <span>{{ 'circularCreate.title' | t }}</span>
          <input type="text" formControlName="title" maxlength="500"/>
        </label>

        <label class="field">
          <span>{{ 'circularCreate.body' | t }}</span>
          <textarea formControlName="body" rows="6"></textarea>
        </label>

        <label class="field field--inline">
          <input type="checkbox" formControlName="broadcast"/>
          <span>{{ 'circularCreate.broadcast' | t }}</span>
        </label>

        <label class="field" *ngIf="!form.controls.broadcast.value">
          <span>{{ 'circularCreate.recipients' | t }}</span>
          <select multiple size="8" formControlName="recipientUserIds">
            <option *ngFor="let u of users" [value]="u.id">{{ displayName(u) }}</option>
          </select>
          <small class="muted">{{ 'circularCreate.recipientsHint' | t }}</small>
        </label>

        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="form.invalid || saving">
            {{ (saving ? 'common.saving' : 'common.send') | t }}
          </button>
          <button type="button" class="btn" (click)="cancel()">{{ 'common.cancel' | t }}</button>
        </div>
      </form>
    </section>
  `,
  styles: [
    `
      .circular-create { max-width: 720px; padding: 1.5rem; }
      .muted { color: var(--text-muted, #6b7280); }
      .circular-create__form { display: flex; flex-direction: column; gap: 1rem; margin-top: 1.5rem; }
      .field { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.9rem; }
      .field--inline { flex-direction: row; align-items: center; gap: 0.5rem; }
      .field input[type=text], .field textarea, .field select {
        padding: 0.5rem 0.6rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db);
        font-family: inherit; font-size: 1rem;
      }
      .actions { display: flex; gap: 0.5rem; }
      .actions .btn { padding: 0.5rem 1.25rem; border-radius: 6px; border: 1px solid var(--border, #d1d5db); cursor: pointer; background: var(--surface, #fff); }
      .actions .btn-primary { background: var(--primary, #0f766e); color: #fff; border-color: transparent; }
    `
  ]
})
export class CircularCreateComponent implements OnInit {
  private readonly api = inject(PlatformCircularApiService);
  private readonly usersApi = inject(UserDirectoryApiService);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(500)]],
    body: ['', Validators.required],
    broadcast: [false],
    recipientUserIds: [[] as string[]]
  });

  users: UserListDto[] = [];
  saving = false;

  ngOnInit(): void {
    this.usersApi.list(0, 500).subscribe({
      next: (p) => (this.users = p?.content ?? []),
      error: () =>
        this.notification.errorRaw(this.i18n.instant('circularCreate.usersLoadError'))
    });
  }

  displayName(u: UserListDto): string {
    const name = this.i18n.currentLang() === 'en' ? u.fullNameEn : u.fullNameAr;
    return `${name} (${u.username})`;
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    const v = this.form.getRawValue();
    const createdBy = this.auth.getUserId() ?? '';
    const body = {
      title: v.title.trim(),
      body: v.body,
      createdBy,
      broadcast: v.broadcast,
      recipientUserIds: v.broadcast ? [] : v.recipientUserIds
    };
    const obs = v.broadcast ? this.api.broadcast(body) : this.api.create(body);
    obs.subscribe({
      next: () => {
        this.notification.successRaw(this.i18n.instant('circularCreate.successMessage'));
        void this.router.navigate(['/circulars']);
      },
      error: () => {
        this.saving = false;
        this.notification.errorRaw(this.i18n.instant('circularCreate.errorMessage'));
      }
    });
  }

  cancel(): void {
    void this.router.navigate(['/circulars']);
  }
}
