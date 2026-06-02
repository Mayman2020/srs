import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  NotificationChannelTargetAdminDto,
  NotificationChannelTargetsApiService
} from '../../core/api/notification-channel-targets-api.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

type ChannelKind = 'EMAIL' | 'WEBHOOK' | 'TEAMS';

const CHANNEL_KINDS: ChannelKind[] = ['EMAIL', 'WEBHOOK', 'TEAMS'];

/**
 * Slice 6 — admin CRUD for notification channel targets. Webhooks and Teams need an `https`
 * target URL and a *reference* to a signing secret env var; raw secrets are never collected or
 * displayed. EMAIL rows just need a `targetCode` to identify the SMTP profile they map to.
 */
@Component({
  selector: 'app-notification-channels-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './notification-channels-admin.component.html',
  styleUrl: './notification-channels-admin.component.scss'
})
export class NotificationChannelsAdminComponent implements OnInit {
  private readonly api = inject(NotificationChannelTargetsApiService);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);
  private readonly fb = inject(FormBuilder);

  readonly rows = signal<NotificationChannelTargetAdminDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly kinds = CHANNEL_KINDS;

  readonly form: FormGroup;

  constructor() {
    this.form = this.fb.group({
      channelCode: ['WEBHOOK' as ChannelKind, [Validators.required]],
      targetCode: ['', [Validators.required, Validators.maxLength(120)]],
      targetUrl: ['', [Validators.maxLength(2048)]],
      signingSecretRef: ['', [Validators.maxLength(120)]],
      enabled: [true],
      description: ['', [Validators.maxLength(255)]]
    });
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().subscribe({
      next: (rows) => {
        this.rows.set(rows ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.rows.set([]);
        this.error.set('notificationAdmin.channels.loadFailed');
        this.loading.set(false);
      }
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      channelCode: 'WEBHOOK',
      targetCode: '',
      targetUrl: '',
      signingSecretRef: '',
      enabled: true,
      description: ''
    });
  }

  startEdit(row: NotificationChannelTargetAdminDto): void {
    this.editingId.set(row.id);
    this.form.reset({
      channelCode: (row.channelCode as ChannelKind) ?? 'WEBHOOK',
      targetCode: row.targetCode,
      targetUrl: row.targetUrl ?? '',
      signingSecretRef: row.signingSecretRef ?? '',
      enabled: row.enabled,
      description: row.description ?? ''
    });
    if (this.editingId() != null) {
      this.form.get('channelCode')?.disable();
      this.form.get('targetCode')?.disable();
    }
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form.enable();
    this.form.reset({
      channelCode: 'WEBHOOK',
      targetCode: '',
      targetUrl: '',
      signingSecretRef: '',
      enabled: true,
      description: ''
    });
  }

  submit(): void {
    if (this.submitting()) {
      return;
    }
    const raw = this.form.getRawValue();
    const channelCode = String(raw.channelCode ?? '').toUpperCase() as ChannelKind;
    const targetUrl = String(raw.targetUrl ?? '').trim();
    const signingSecretRef = String(raw.signingSecretRef ?? '').trim();

    if (channelCode === 'WEBHOOK' || channelCode === 'TEAMS') {
      if (!this.isHttpsUrl(targetUrl)) {
        this.toast.error('notificationAdmin.channels.errorUrl');
        return;
      }
      if (signingSecretRef.length === 0 || !this.isEnvVarRef(signingSecretRef)) {
        this.toast.error('notificationAdmin.channels.errorSecretRef');
        return;
      }
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('common.formInvalid');
      return;
    }

    this.submitting.set(true);
    const editId = this.editingId();
    const obs = editId
      ? this.api.update(editId, {
          targetUrl: targetUrl || null,
          signingSecretRef: signingSecretRef || null,
          enabled: Boolean(raw.enabled),
          description: this.normalizeOpt(raw.description as string)
        })
      : this.api.create({
          channelCode,
          targetCode: String(raw.targetCode ?? '').trim(),
          targetUrl: targetUrl || null,
          signingSecretRef: signingSecretRef || null,
          enabled: Boolean(raw.enabled),
          description: this.normalizeOpt(raw.description as string)
        });

    obs.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(
          editId ? 'notificationAdmin.channels.updatedToast' : 'notificationAdmin.channels.createdToast'
        );
        this.cancelEdit();
        this.refresh();
      },
      error: (err: { error?: { message?: string; detail?: string } }) => {
        this.submitting.set(false);
        const msg = err?.error?.message ?? err?.error?.detail ?? null;
        if (msg) {
          this.toast.errorRaw(msg);
        } else {
          this.toast.error('notificationAdmin.channels.saveFailedToast');
        }
      }
    });
  }

  confirmDelete(row: NotificationChannelTargetAdminDto): void {
    this.dialog
      .openConfirm({
        titleKey: 'notificationAdmin.channels.deleteConfirmTitle',
        messageKey: 'notificationAdmin.channels.deleteConfirmMessage',
        params: { code: row.targetCode },
        confirmButton: { labelKey: 'common.delete', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.delete(row.id).subscribe({
          next: () => {
            this.toast.success('notificationAdmin.channels.deletedToast');
            if (this.editingId() === row.id) {
              this.cancelEdit();
            }
            this.refresh();
          },
          error: () => this.toast.error('notificationAdmin.channels.deleteFailedToast')
        });
      });
  }

  toggleEnabled(row: NotificationChannelTargetAdminDto): void {
    this.api.update(row.id, { enabled: !row.enabled }).subscribe({
      next: () => {
        this.toast.success('notificationAdmin.channels.toggledToast');
        this.refresh();
      },
      error: () => this.toast.error('notificationAdmin.channels.saveFailedToast')
    });
  }

  channelLabel(code: string): string {
    return `notificationAdmin.channels.kind.${code}`;
  }

  needsUrl(channelCode: string): boolean {
    return channelCode === 'WEBHOOK' || channelCode === 'TEAMS';
  }

  private normalizeOpt(s: string | null | undefined): string | null {
    if (s == null) return null;
    const v = String(s).trim();
    return v.length === 0 ? null : v;
  }

  private isHttpsUrl(s: string): boolean {
    try {
      const u = new URL(s);
      return u.protocol === 'https:' || u.protocol === 'http:';
    } catch {
      return false;
    }
  }

  private isEnvVarRef(s: string): boolean {
    return /^[A-Z][A-Z0-9_.-]{1,118}$/.test(s);
  }
}
