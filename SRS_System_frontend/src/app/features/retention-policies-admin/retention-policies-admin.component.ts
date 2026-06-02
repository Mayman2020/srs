import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  RetentionAdminApiService,
  RetentionPolicyAdminDto
} from '../../core/api/retention-admin-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

/**
 * Slice 6 — retention policies admin screen. Lists policies seeded by Flyway and lets operators
 * toggle each one on/off (the only mutation the backend exposes; create/edit/delete are
 * intentionally not allowed at runtime because policies map to compliance/legal requirements
 * and live in version control).
 */
@Component({
  selector: 'app-retention-policies-admin',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './retention-policies-admin.component.html',
  styleUrl: './retention-policies-admin.component.scss'
})
export class RetentionPoliciesAdminComponent implements OnInit {
  private readonly api = inject(RetentionAdminApiService);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);
  private readonly i18n = inject(I18nService);

  readonly rows = signal<RetentionPolicyAdminDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listPolicies().subscribe({
      next: (rows) => {
        this.rows.set(rows ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.rows.set([]);
        this.error.set('retention.policies.loadFailed');
        this.loading.set(false);
      }
    });
  }

  policyLabel(p: RetentionPolicyAdminDto): string {
    return this.i18n.currentLang() === 'en' ? p.nameEn : p.nameAr;
  }

  toggle(p: RetentionPolicyAdminDto): void {
    const turningOff = p.enabled;
    const apply = () => {
      this.api.togglePolicy(p.id, { enabled: !p.enabled }).subscribe({
        next: () => {
          this.toast.success(
            turningOff ? 'retention.policies.disabledToast' : 'retention.policies.enabledToast'
          );
          this.refresh();
        },
        error: () => this.toast.error('retention.policies.saveFailedToast')
      });
    };

    if (!turningOff) {
      apply();
      return;
    }
    this.dialog
      .openConfirm({
        titleKey: 'retention.policies.disableConfirmTitle',
        messageKey: 'retention.policies.disableConfirmMessage',
        params: { code: p.code },
        confirmButton: { labelKey: 'retention.policies.disable', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (ok) {
          apply();
        }
      });
  }

  actionLabel(code: string): string {
    return `retention.policies.action.${code}`;
  }
}
