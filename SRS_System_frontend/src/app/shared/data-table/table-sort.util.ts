/** Shared client-side sort helpers for list tables. */

export type SortDirection = 'asc' | 'desc';

export function normalizeSortableString(value: unknown): string {
  if (value == null) {
    return '';
  }
  return String(value).trim().toLowerCase();
}

export function compareSortValues(a: unknown, b: unknown, dir: SortDirection): number {
  const av = a;
  const bv = b;

  if (av == null && bv == null) {
    return 0;
  }
  if (av == null) {
    return dir === 'asc' ? 1 : -1;
  }
  if (bv == null) {
    return dir === 'asc' ? -1 : 1;
  }

  if (typeof av === 'number' && typeof bv === 'number' && !Number.isNaN(av) && !Number.isNaN(bv)) {
    return dir === 'asc' ? av - bv : bv - av;
  }

  const as = normalizeSortableString(av);
  const bs = normalizeSortableString(bv);
  const cmp = as.localeCompare(bs, undefined, { numeric: true, sensitivity: 'base' });
  return dir === 'asc' ? cmp : -cmp;
}
