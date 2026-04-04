import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { I18nService } from '../i18n/i18n.service';

/** Maps HTTP failures to user-visible messages; rethrows for feature-level handling. */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const i18n = inject(I18nService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        const msg = resolveMessage(err, i18n);
        (err as HttpErrorResponse & { userMessage?: string }).userMessage = msg;
      }
      return throwError(() => err);
    })
  );
};

function resolveMessage(err: HttpErrorResponse, i18n: I18nService): string {
  if (err.status === 0) {
    return i18n.instant('errors.network');
  }
  if (err.status === 401) {
    return i18n.instant('errors.unauthorized');
  }
  if (err.status === 403) {
    const b = err.error;
    if (typeof b === 'string' && b.trim()) return b;
    return i18n.instant('errors.forbidden');
  }
  if (err.status === 404) {
    const b = err.error;
    if (typeof b === 'string' && b.trim()) return b;
    return i18n.instant('errors.notFound');
  }
  if (err.status === 400) {
    if (err.error && typeof err.error === 'object' && !Array.isArray(err.error)) {
      const vals = Object.values(err.error as Record<string, string>);
      if (vals.length) return vals.join(' ');
    }
    if (typeof err.error === 'string') return err.error;
  }
  if (err.status >= 500) {
    return i18n.instant('errors.server');
  }
  return i18n.instant('errors.generic');
}
