import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { SrsDatePipe } from '../../pipes/srs-date.pipe';
import { UserAuditRefDto } from '../../../core/api/api-types';

@Component({
  selector: 'app-audit-trail',
  standalone: true,
  imports: [CommonModule, TranslatePipe, SrsDatePipe],
  template: `
    <div class="srs-audit-trail" *ngIf="hasAny">
      <div class="srs-audit-trail__title">
        <span class="material-icons" aria-hidden="true">history</span>
        {{ 'auditTrail.infoTitle' | t }}
      </div>
      <div class="srs-audit-trail__grid">
        <div class="srs-audit-trail__cell" *ngIf="showCreatedRow">
          <span class="srs-audit-trail__label">{{ 'auditTrail.createdBy' | t }}</span>
          <span class="srs-audit-trail__value">{{ createdByLabel }}</span>
        </div>
        <div class="srs-audit-trail__cell" *ngIf="showCreatedRow">
          <span class="srs-audit-trail__label">{{ 'auditTrail.createdAt' | t }}</span>
          <span class="srs-audit-trail__value">{{ createdAt ? (createdAt | srsDate:'datetime') : '—' }}</span>
        </div>
        <div class="srs-audit-trail__cell" *ngIf="showModifiedRow">
          <span class="srs-audit-trail__label">{{ 'auditTrail.modifiedBy' | t }}</span>
          <span class="srs-audit-trail__value">{{ modifiedByLabel }}</span>
        </div>
        <div class="srs-audit-trail__cell" *ngIf="showModifiedRow">
          <span class="srs-audit-trail__label">{{ 'auditTrail.updatedAt' | t }}</span>
          <span class="srs-audit-trail__value">{{ updatedAt ? (updatedAt | srsDate:'datetime') : '—' }}</span>
        </div>
        <div class="srs-audit-trail__cell srs-audit-trail__cell--wide" *ngIf="approvedByName || approvedBy">
          <span class="srs-audit-trail__label">{{ 'auditTrail.approvedBy' | t }}</span>
          <span class="srs-audit-trail__value">{{ approvedByName || ('auditTrail.unknown' | t) }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        width: 100%;
      }
      .srs-audit-trail {
        margin-top: 12px;
        padding: 12px 14px;
        border-radius: 8px;
        background: color-mix(in srgb, var(--srs-green, #0b6e4f) 6%, var(--surface-card, #fff));
        border: 1px solid color-mix(in srgb, var(--srs-green, #0b6e4f) 18%, transparent);
      }
      .srs-audit-trail__title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        margin-bottom: 10px;
        color: var(--text-primary, #1f2937);
        font-size: 14px;
      }
      .srs-audit-trail__title .material-icons {
        font-size: 18px;
        color: var(--srs-green, #0b6e4f);
      }
      .srs-audit-trail__grid {
        display: grid;
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        column-gap: 24px;
        row-gap: 10px;
      }
      .srs-audit-trail__cell {
        display: flex;
        flex-direction: column;
        gap: 3px;
        min-width: 0;
      }
      .srs-audit-trail__cell--wide {
        grid-column: 1 / -1;
      }
      .srs-audit-trail__label {
        font-size: 12px;
        opacity: 0.72;
      }
      .srs-audit-trail__value {
        font-size: 13px;
        font-weight: 500;
        word-break: break-word;
      }
      @media (max-width: 520px) {
        .srs-audit-trail__grid {
          grid-template-columns: 1fr;
        }
        .srs-audit-trail__cell--wide {
          grid-column: auto;
        }
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SrsAuditTrailComponent {
  @Input() createdAt?: string | null;
  @Input() updatedAt?: string | null;
  @Input() createdBy?: string | null;
  @Input() createdByName?: string | null;
  @Input() modifiedBy?: string | null;
  @Input() modifiedByName?: string | null;
  @Input() approvedBy?: string | null;
  @Input() approvedByName?: string | null;
  @Input() createdByUser?: UserAuditRefDto | null;
  @Input() updatedByUser?: UserAuditRefDto | null;

  get showCreatedRow(): boolean {
    return !!(this.createdAt || this.createdByName || this.createdBy || this.createdByUser);
  }

  get showModifiedRow(): boolean {
    return !!(this.updatedAt || this.modifiedByName || this.modifiedBy || this.updatedByUser);
  }

  get createdByLabel(): string {
    const name = (this.createdByName ?? this.auditUserName(this.createdByUser) ?? '').trim();
    if (name) return name;
    if (this.createdBy?.trim()) return `#${this.createdBy.trim()}`;
    if (this.createdByUser?.id) return `#${this.createdByUser.id}`;
    return '—';
  }

  get modifiedByLabel(): string {
    const name = (this.modifiedByName ?? this.auditUserName(this.updatedByUser) ?? '').trim();
    if (name) return name;
    if (this.modifiedBy?.trim()) return `#${this.modifiedBy.trim()}`;
    if (this.updatedByUser?.id) return `#${this.updatedByUser.id}`;
    return '—';
  }

  get hasAny(): boolean {
    return this.showCreatedRow || this.showModifiedRow || !!(this.approvedByName || this.approvedBy);
  }

  private auditUserName(ref: UserAuditRefDto | null | undefined): string {
    if (!ref) return '';
    return (ref.fullNameAr?.trim() || ref.fullNameEn?.trim() || '').trim();
  }
}
