import type { SrsUiSortDirection } from './srs-server-table.types';

/** Cell rendering strategy for configurable / metadata-driven tables. */
export type SrsColumnCellType = 'text' | 'date' | 'lookup' | 'number' | 'actions';

/**
 * Declarative column metadata. Screens can drive thead generation and server sort mapping
 * without duplicating header label keys.
 */
export interface SrsTableColumnMeta {
  id: string;
  /** i18n key for header (via `t` pipe). */
  headerKey: string;
  cellType: SrsColumnCellType;
  /** DTO field for simple text/date/number cells. */
  field?: string;
  /** Lookup group for `lk` pipe when cellType is `lookup`. */
  lookupGroup?: string;
  sortable?: boolean;
  /**
   * Spring-sortable property name (whitelist on backend), e.g. `createdAt`, `subject`.
   * When omitted and sortable is true, `id` is used if it matches API whitelist.
   */
  sortProperty?: string;
  align?: 'start' | 'center' | 'end';
  width?: string;
}

/** Build Spring `sort` query params from UI state. */
export function springSortParam(property: string, direction: SrsUiSortDirection): string {
  return `${property},${direction}`;
}
