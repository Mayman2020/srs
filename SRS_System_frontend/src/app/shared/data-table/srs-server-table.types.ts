/**
 * Shared contracts for server-driven tables (pagination / sort / filter requests).
 * Feature components map these to HTTP query params (e.g. Spring Pageable).
 */
export type SrsPaginationMode = 'client' | 'server';

/** Emitted when the user changes page in server mode (1-based page index). */
export interface SrsServerPageChange {
  page: number;
  pageSize: number;
}

/** Sort direction for UI; map to Spring `property,direction` elsewhere. */
export type SrsUiSortDirection = 'asc' | 'desc';

export interface SrsServerSortChange {
  /** Logical column id from the screen (e.g. `created`). */
  columnId: string;
  direction: SrsUiSortDirection;
  /** Ready-to-send Spring sort strings, e.g. `['createdAt,desc']`. */
  sortParams: string[];
}

/**
 * How {@link SrsDataTableComponent} shows loading:
 * - skeleton: hide table, show skeleton (default; best for first paint)
 * - overlay: keep previous rows visible with a dimmed overlay (best for page/sort refetch)
 */
export type SrsTableLoadingMode = 'skeleton' | 'overlay';
