import { ChangeDetectorRef } from '@angular/core';
import { finalize } from 'rxjs';
import type { Observable, Subscription } from 'rxjs';

export type SubscribePageLoadOptions<T> = {
  cdr: ChangeDetectorRef;
  source: Observable<T>;
  /**
   * When set: `true` before the request, `false` in `finalize` (success, error, or unsubscribe).
   * Omit for secondary requests (e.g. reference data) where no loading row is shown.
   */
  setLoading?: (loading: boolean) => void;
  next: (value: T) => void;
  error?: (err: unknown) => void;
};

/**
 * Standard enterprise page-load wiring for **Default** `ChangeDetectionStrategy` hosts that embed
 * **OnPush** children (e.g. `srs-data-table`): loading flag + `ChangeDetectorRef.detectChanges()` on
 * every phase so the tree updates without waiting for a user event.
 */
export function subscribePageLoad<T>(opts: SubscribePageLoadOptions<T>): Subscription {
  const { cdr, source, setLoading, next, error } = opts;
  if (setLoading) {
    setLoading(true);
  }
  cdr.detectChanges();

  return source
    .pipe(
      finalize(() => {
        if (setLoading) {
          setLoading(false);
        }
        cdr.detectChanges();
      })
    )
    .subscribe({
      next: (value) => {
        next(value);
        cdr.detectChanges();
      },
      error: (err) => {
        error?.(err);
        cdr.detectChanges();
      },
    });
}
