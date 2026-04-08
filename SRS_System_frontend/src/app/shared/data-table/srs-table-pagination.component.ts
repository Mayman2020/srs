import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { SRS_TABLE_DEFAULT_PAGE_SIZE, SRS_TABLE_PAGE_SIZE_OPTIONS } from './srs-table-defaults';

@Component({
  selector: 'srs-table-pagination',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './srs-table-pagination.component.html',
  styleUrl: './srs-table-pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SrsTablePaginationComponent implements OnChanges {
  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnChanges(_changes: SimpleChanges): void {
    this.cdr.markForCheck();
  }

  @Input() page = 1;
  @Input() pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  @Input() total = 0;
  @Input() pageSizeOptions: number[] = [...SRS_TABLE_PAGE_SIZE_OPTIONS];
  /** When true, show the legacy "Showing a of b" line (a = rows on current page). */
  @Input() legacyCountOnPage = 0;

  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();

  get totalPages(): number {
    if (this.total <= 0) {
      return 1;
    }
    return Math.ceil(this.total / this.pageSize);
  }

  get rangeStart(): number {
    if (this.total === 0) {
      return 0;
    }
    return (this.page - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.page * this.pageSize, this.total);
  }

  pageItems(): (number | 'ellipsis')[] {
    const tp = this.totalPages;
    if (tp <= 1) {
      return [1];
    }
    if (tp <= 7) {
      return Array.from({ length: tp }, (_, i) => i + 1);
    }
    const p = this.page;
    const set = new Set<number>();
    set.add(1);
    set.add(tp);
    for (let i = p - 1; i <= p + 1; i++) {
      if (i >= 1 && i <= tp) {
        set.add(i);
      }
    }
    const sorted = [...set].sort((a, b) => a - b);
    const out: (number | 'ellipsis')[] = [];
    let prev = 0;
    for (const n of sorted) {
      if (prev && n - prev > 1) {
        out.push('ellipsis');
      }
      out.push(n);
      prev = n;
    }
    return out;
  }

  prev(): void {
    if (this.page > 1) {
      this.pageChange.emit(this.page - 1);
    }
  }

  next(): void {
    if (this.page < this.totalPages) {
      this.pageChange.emit(this.page + 1);
    }
  }

  go(n: number): void {
    if (n >= 1 && n <= this.totalPages && n !== this.page) {
      this.pageChange.emit(n);
    }
  }

  trackByPageItem(index: number, item: number | 'ellipsis'): string {
    return item === 'ellipsis' ? `e-${index}` : String(item);
  }
}
