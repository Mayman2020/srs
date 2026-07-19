import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LegalHoldDto,
  RetentionAdminApiService
} from '../../core/api/retention-admin-api.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

/**
 * Slice 6 — legal holds admin. Lists active holds and lets operators place a new hold (any
 * correspondence UUID + reason) or release an existing one (with a release reason). Both
 * mutations require `LEGAL_HOLD_MANAGE` server-side; the release flow uses a confirm dialog
 * because the action permanently lifts the retention freeze.
 */
@Component({
  selector: 'app-legal-holds-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslatePipe, SrsDatePipe],
  templateUrl: './legal-holds-admin.component.html',
  styleUrl: './legal-holds-admin.component.scss'
})
export class LegalHoldsAdminComponent implements OnInit {
  private readonly api = inject(RetentionAdminApiService);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);
  private readonly fb = inject(FormBuilder);

  readonly rows = signal<LegalHoldDto[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form: FormGroup;
  releasingId: string | null = null;
  releaseReason = '';

  private readonly uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  constructor() {
    this.form = this.fb.group({
      correspondenceId: [
        '',
        [Validators.required, Validators.pattern(this.uuidRegex)]
      ],
      reason: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listActiveLegalHolds().subscribe({
      next: (rows) => {
        this.rows.set(rows ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.rows.set([]);
        this.error.set('retention.legalHolds.loadFailed');
        this.loading.set(false);
      }
    });
  }

  place(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.api
      .placeLegalHold({
        correspondenceId: String(raw.correspondenceId).trim(),
        reason: String(raw.reason).trim()
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('retention.legalHolds.placedToast');
          this.form.reset();
          this.refresh();
        },
        error: (err: { error?: { message?: string; detail?: string } }) => {
          this.submitting.set(false);
          const msg = err?.error?.message ?? err?.error?.detail ?? null;
          if (msg) {
            this.toast.errorRaw(msg);
          } else {
            this.toast.error('retention.legalHolds.placeFailedToast');
          }
        }
      });
  }

  startRelease(row: LegalHoldDto): void {
    this.releasingId = row.id;
    this.releaseReason = '';
  }

  cancelRelease(): void {
    this.releasingId = null;
    this.releaseReason = '';
  }

  confirmRelease(row: LegalHoldDto): void {
    const reason = (this.releaseReason ?? '').trim();
    if (reason.length < 3) {
      this.toast.error('retention.legalHolds.releaseReasonRequired');
      return;
    }
    this.dialog
      .openConfirm({
        titleKey: 'retention.legalHolds.releaseConfirmTitle',
        messageKey: 'retention.legalHolds.releaseConfirmMessage',
        confirmButton: { labelKey: 'retention.legalHolds.release', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.releaseLegalHold(row.id, { releaseReason: reason }).subscribe({
          next: () => {
            this.toast.success('retention.legalHolds.releasedToast');
            this.cancelRelease();
            this.refresh();
          },
          error: () => this.toast.error('retention.legalHolds.releaseFailedToast')
        });
      });
  }
}
