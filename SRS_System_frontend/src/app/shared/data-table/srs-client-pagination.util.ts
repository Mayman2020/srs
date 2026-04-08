import { SRS_TABLE_DEFAULT_PAGE_SIZE } from './srs-table-defaults';

/**
 * Client-side page slicing + page clamping after filter/sort.
 * For server-side tables (`paginationMode="server"`), the parent loads `reportRows` (or equivalent)
 * from the API and sets `total` from the Spring page; this helper is not used.
 */

export interface SrsClientPaginateResult<T> {
  /** Rows for the current page (may be shorter than pageSize on last page). */
  pageRows: T[];
  /** Total rows in the full (filtered / sorted) source list. */
  total: number;
  /** Clamped 1-based page index (use to sync component state when out of range). */
  page: number;
}

/**
 * Client-side pagination: clamp page to valid range, then slice.
 * Use after filter/sort to keep page in range and avoid rendering unbounded lists.
 */
export function srsClientPaginate<T>(
  sortedOrFilteredRows: T[],
  page: number,
  pageSize: number = SRS_TABLE_DEFAULT_PAGE_SIZE
): SrsClientPaginateResult<T> {
  const size = Math.max(1, pageSize);
  const total = sortedOrFilteredRows.length;
  const totalPages = Math.max(1, Math.ceil(total / size));
  const clampedPage = Math.min(Math.max(1, page), totalPages);
  const start = (clampedPage - 1) * size;
  return {
    pageRows: sortedOrFilteredRows.slice(start, start + size),
    total,
    page: clampedPage
  };
}
