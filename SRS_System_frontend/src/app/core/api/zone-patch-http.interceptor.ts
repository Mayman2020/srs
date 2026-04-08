import { HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { ApplicationRef, inject, isDevMode, NgZone } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * 1) Re-delivers HttpClient notifications inside `NgZone` (XHR/fetch may fire outside Zone.js).
 * 2) Calls `ApplicationRef.tick()` on terminal response/error so the view refreshes even when
 *    the zone does not schedule a change detection pass (common with some Angular 21 + tooling stacks).
 *
 * Symptom without this: loading skeletons stay until any click (patched DOM event runs CD).
 */
export const zonePatchHttpInterceptor: HttpInterceptorFn = (req, next) => {
  const ngZone = inject(NgZone);
  const appRef = inject(ApplicationRef);
  const debug = isDevMode();

  return new Observable((subscriber) => {
    const sub = next(req).subscribe({
      next: (value) => {
        ngZone.run(() => {
          subscriber.next(value);
          if (value.type === HttpEventType.Response) {
            if (debug) {
              console.debug('[HttpZonePatch] Response', req.method, req.urlWithParams);
            }
            appRef.tick();
          }
        });
      },
      error: (err) => {
        ngZone.run(() => {
          if (debug) {
            console.debug('[HttpZonePatch] Error', req.method, req.urlWithParams, err);
          }
          subscriber.error(err);
          appRef.tick();
        });
      },
      complete: () => {
        ngZone.run(() => subscriber.complete());
      },
    });
    return () => sub.unsubscribe();
  });
};
