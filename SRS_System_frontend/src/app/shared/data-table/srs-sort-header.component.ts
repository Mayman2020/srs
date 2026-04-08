import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import type { SortDirection } from './table-sort.util';

@Component({
  selector: 'srs-sort-header',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    <button
      type="button"
      class="srs-sort-header"
      [class.srs-sort-header--active]="active"
      (click)="onClick()"
      [attr.aria-label]="ariaLabel"
    >
      <span class="srs-sort-header__label">{{ labelKey | t: labelParams }}</span>
      <span class="srs-sort-header__icons" aria-hidden="true">
        <i class="fa-solid fa-chevron-up srs-sort-header__chev" [class.srs-sort-header__chev--on]="active && direction === 'asc'"></i>
        <i class="fa-solid fa-chevron-down srs-sort-header__chev" [class.srs-sort-header__chev--on]="active && direction === 'desc'"></i>
      </span>
    </button>
  `,
  styles: [
    `
      .srs-sort-header {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        width: 100%;
        margin: 0;
        padding: 4px 2px;
        border: none;
        background: transparent;
        font: inherit;
        font-weight: 600;
        color: inherit;
        text-align: inherit;
        cursor: pointer;
        border-radius: 6px;
        transition: background-color 0.15s ease, color 0.15s ease;
      }
      .srs-sort-header:hover {
        background: rgba(11, 110, 79, 0.08);
      }
      .srs-sort-header--active {
        color: var(--primary-dark, #0b6e4f);
      }
      .srs-sort-header__label {
        flex: 1;
        min-width: 0;
      }
      .srs-sort-header__icons {
        display: inline-flex;
        flex-direction: column;
        line-height: 1;
        gap: 0;
        opacity: 0.35;
      }
      .srs-sort-header--active .srs-sort-header__icons {
        opacity: 1;
      }
      .srs-sort-header__chev {
        font-size: 9px;
        display: block;
      }
      .srs-sort-header__chev--on {
        color: var(--primary-green, #0b6e4f);
        opacity: 1;
      }
    `,
  ],
})
export class SrsSortHeaderComponent {
  @Input({ required: true }) columnId!: string;
  @Input({ required: true }) labelKey!: string;
  @Input() labelParams?: Record<string, string | number>;
  @Input() activeColumn = '';
  @Input() direction: SortDirection = 'asc';

  @Output() sortChange = new EventEmitter<{ columnId: string; direction: SortDirection }>();

  constructor(private readonly i18n: I18nService) {}

  get active(): boolean {
    return this.activeColumn === this.columnId;
  }

  get ariaLabel(): string {
    const col = this.i18n.instant(this.labelKey, this.labelParams);
    if (!this.active) {
      return this.i18n.instant('dataTable.sortByColumn', { col });
    }
    return this.direction === 'asc'
      ? this.i18n.instant('dataTable.sortedAscending', { col })
      : this.i18n.instant('dataTable.sortedDescending', { col });
  }

  onClick(): void {
    const nextDir: SortDirection =
      this.activeColumn === this.columnId ? (this.direction === 'asc' ? 'desc' : 'asc') : 'asc';
    this.sortChange.emit({ columnId: this.columnId, direction: nextDir });
  }
}
