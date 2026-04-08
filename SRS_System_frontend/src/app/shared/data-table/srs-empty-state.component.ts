import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

@Component({
  selector: 'srs-empty-state',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  template: `
    <div class="srs-empty" role="status">
      <div class="srs-empty__icon" *ngIf="showIcon" aria-hidden="true">
        <i class="fa-solid fa-inbox"></i>
      </div>
      <p class="srs-empty__title">{{ titleKey | t: titleParams }}</p>
      <p class="srs-empty__hint" *ngIf="hintKey">{{ hintKey | t: hintParams }}</p>
    </div>
  `,
  styles: [
    `
      .srs-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 28px 16px;
        text-align: center;
        color: #64748b;
      }
      .srs-empty__icon {
        font-size: 2rem;
        opacity: 0.45;
        color: #0b6e4f;
      }
      .srs-empty__title {
        margin: 0;
        font-size: 0.95rem;
        font-weight: 600;
        color: #475569;
      }
      .srs-empty__hint {
        margin: 0;
        font-size: 0.85rem;
        max-width: 360px;
        line-height: 1.45;
      }
    `,
  ],
})
export class SrsEmptyStateComponent {
  @Input({ required: true }) titleKey!: string;
  @Input() titleParams?: Record<string, string | number>;
  @Input() hintKey?: string;
  @Input() hintParams?: Record<string, string | number>;
  @Input() showIcon = true;
}
