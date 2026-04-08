import { finalize } from 'rxjs';
import type { MonoTypeOperatorFunction } from 'rxjs';

/**
 * @deprecated Prefer {@link subscribePageLoad} on feature pages so loading + `detectChanges()` stay consistent.
 * Kept for rare pipe-only use outside components.
 */
export function finalizeLoading<T>(clearLoading: () => void): MonoTypeOperatorFunction<T> {
  return finalize(clearLoading);
}

export { subscribePageLoad, type SubscribePageLoadOptions } from './subscribe-page-load';
