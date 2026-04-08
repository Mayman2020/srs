import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SrsTableSkeletonComponent } from './srs-table-skeleton.component';
import { SrsTablePaginationComponent } from './srs-table-pagination.component';
import type { SrsPaginationMode, SrsTableLoadingMode } from './srs-server-table.types';
import { SRS_TABLE_DEFAULT_PAGE_SIZE, SRS_TABLE_PAGE_SIZE_OPTIONS } from './srs-table-defaults';

/**
 * Presentational table shell: no API calls, no sorting/filtering logic.
 * Containers own row data, client/server paging, and pass state via `@Input()` + events.
 * Projected `<table>` content is authored by feature modules (smart components).
 */
@Component({
  selector: 'srs-data-table',
  standalone: true,
  imports: [CommonModule, SrsTableSkeletonComponent, SrsTablePaginationComponent],
  templateUrl: './srs-data-table.component.html',
  styleUrl: './srs-data-table.component.scss',
  encapsulation: ViewEncapsulation.None,
  host: { class: 'srs-dt-host' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SrsDataTableComponent implements OnChanges {
  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnChanges(_changes: SimpleChanges): void {
    // Host is OnPush; parent async updates (loading, totals, page) must schedule this branch.
    this.cdr.markForCheck();
  }
  @Input() loading = false;
  @Input() showSkeleton = true;
  @Input() skeletonRows = 6;
  @Input() skeletonColumns = 6;
  /** Translated string for skeleton status region. */
  @Input() skeletonAriaLabel = '';

  /**
   * `skeleton` (default): replace table with skeleton while loading.
   * `overlay`: keep table visible and dim with overlay (server page/sort refetch).
   */
  @Input() loadingMode: SrsTableLoadingMode = 'skeleton';

  /** `server`: total count and paging are owned by the backend page. */
  @Input() paginationMode: SrsPaginationMode = 'client';

  /** When true, pager stays visible during loading (typical for server mode + overlay). */
  @Input() preservePaginationWhileLoading = false;

  /** Set false only for rare tables that must not show a pager. */
  @Input() showPagination = true;
  @Input() page = 1;
  @Input() pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  @Input() total = 0;
  @Input() pageSizeOptions: number[] = [...SRS_TABLE_PAGE_SIZE_OPTIONS];
  @Input() legacyCountOnPage = 0;

  @Output() paginationPage = new EventEmitter<number>();
  @Output() paginationPageSize = new EventEmitter<number>();

  get useOverlay(): boolean {
    return this.loadingMode === 'overlay';
  }

  get showSkeletonLayer(): boolean {
    return this.showSkeleton && this.loading && !this.useOverlay;
  }

  get ghostInner(): boolean {
    return this.showSkeleton && this.loading && !this.useOverlay;
  }
}
