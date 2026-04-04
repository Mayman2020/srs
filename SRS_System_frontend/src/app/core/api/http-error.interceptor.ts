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
    if (err.url?.includes('/auth/login')) {
      return i18n.instant('errors.badCredentials');
    }
    return i18n.instant('errors.unauthorized');
  }
  if (err.status === 403) {
    const body = extractErrorBodyText(err);
    if (body && isCorsOrSecurityNoise(body)) {
      return i18n.instant('errors.cors');
    }
    if (body && !isTechnicalJargon(body)) {
      return body;
    }
    return i18n.instant('errors.forbidden');
  }
  if (err.status === 404) {
    const body = extractErrorBodyText(err);
    if (body && !isTechnicalJargon(body)) {
      return body;
    }
    return i18n.instant('errors.notFound');
  }
  if (err.status === 400) {
    if (err.error && typeof err.error === 'object' && !Array.isArray(err.error)) {
      const vals = Object.values(err.error as Record<string, string>);
      if (vals.length) return vals.join(' ');
    }
    const body = extractErrorBodyText(err);
    if (body && !isTechnicalJargon(body)) {
      return body;
    }
  }
  if (err.status >= 500) {
    return i18n.instant('errors.server');
  }
  const fallback = extractErrorBodyText(err);
  if (fallback && !isTechnicalJargon(fallback)) {
    return fallback;
  }
  return i18n.instant('errors.generic');
}

function extractErrorBodyText(err: HttpErrorResponse): string | null {
  const e = err.error;
  if (typeof e === 'string' && e.trim()) {
    return e.trim();
  }
  if (e && typeof e === 'object' && !Array.isArray(e)) {
    const o = e as Record<string, unknown>;
    const msg = o['message'];
    const errField = o['error'];
    if (typeof msg === 'string' && msg.trim()) {
      return msg.trim();
    }
    if (typeof errField === 'string' && errField.trim()) {
      return errField.trim();
    }
  }
  return null;
}

/** Hide Spring / infra phrases from end users. */
function isTechnicalJargon(s: string): boolean {
  return /cors|csrf|xss|forbidden|unauthorized|nullpointer|stack trace|exception|internal server/i.test(
    s
  );
}

function isCorsOrSecurityNoise(s: string): boolean {
  return /cors|invalid cors|cross-origin/i.test(s);
}
