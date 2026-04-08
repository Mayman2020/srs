import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/**
 * Reusable filter panel: title row, projected controls, optional clear/apply actions.
 * All labels via translation keys.
 */
@Component({
  selector: 'srs-filter-bar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  template: `
    <div class="srs-filter-bar">
      <div class="srs-filter-bar__head" *ngIf="titleKey">
        <h3 class="srs-filter-bar__title">{{ titleKey | t: titleParams }}</h3>
        <p class="srs-filter-bar__subtitle" *ngIf="subtitleKey">{{ subtitleKey | t: subtitleParams }}</p>
      </div>
      <div class="srs-filter-bar__grid">
        <ng-content></ng-content>
      </div>
      <div class="srs-filter-bar__actions" *ngIf="showClear || showApply">
        <button *ngIf="showClear" type="button" class="btn ghost" (click)="clear.emit()">
          {{ clearKey | t }}
        </button>
        <button *ngIf="showApply" type="button" class="btn primary" (click)="apply.emit()">
          {{ applyKey | t }}
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .srs-filter-bar {
        display: flex;
        flex-direction: column;
        gap: 14px;
      }
      .srs-filter-bar__head {
        margin-bottom: 2px;
      }
      .srs-filter-bar__title {
        margin: 0;
        font-size: 1rem;
        color: var(--primary-dark, #064635);
      }
      .srs-filter-bar__subtitle {
        margin: 4px 0 0;
        font-size: 0.85rem;
        color: #64748b;
      }
      .srs-filter-bar__grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
        gap: 12px 14px;
        align-items: end;
      }
      .srs-filter-bar__grid ::ng-deep label {
        display: flex;
        flex-direction: column;
        gap: 6px;
        font-size: 0.8rem;
        font-weight: 600;
        color: #374151;
      }
      .srs-filter-bar__grid ::ng-deep input,
      .srs-filter-bar__grid ::ng-deep select {
        padding: 8px 10px;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
        font: inherit;
        background: #fff;
      }
      .srs-filter-bar__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        align-items: center;
      }
    `,
  ],
})
export class SrsFilterBarComponent {
  @Input() titleKey = '';
  @Input() titleParams?: Record<string, string | number>;
  @Input() subtitleKey = '';
  @Input() subtitleParams?: Record<string, string | number>;

  @Input() showClear = false;
  @Input() showApply = false;
  @Input() clearKey = 'common.clear';
  @Input() applyKey = 'common.apply';

  @Output() clear = new EventEmitter<void>();
  @Output() apply = new EventEmitter<void>();
}
