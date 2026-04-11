import type { LookupItemDto } from '../api/api-types';

/** Allowed `correspondence_status.ui_variant` values (Flyway V29). */
const ALLOWED = new Set(['success', 'danger', 'warning', 'info', 'secondary', 'neutral']);

/**
 * Normalizes API `uiVariant` to a CSS suffix for `badge-${value}`.
 * Unknown or missing values become `neutral` (no status-code heuristics).
 */
export function correspondenceStatusBadgeClass(uiVariant: string | null | undefined): string {
  const v = (uiVariant ?? 'neutral').toLowerCase().trim();
  if (ALLOWED.has(v)) {
    return v;
  }
  return 'neutral';
}

/** Resolve `ui_variant` from the lookup bundle when only a status code is available. */
export function uiVariantForStatusCode(
  statuses: readonly LookupItemDto[] | null | undefined,
  code: string | null | undefined
): string | null {
  if (!code?.trim() || !statuses?.length) {
    return null;
  }
  return statuses.find((x) => x.code === code)?.uiVariant ?? null;
}
