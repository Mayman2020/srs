import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { I18nService } from '../i18n/i18n.service';
import {
  ERROR_MESSAGE_KEY,
  SKIP_ERROR_NOTIFICATION,
} from './http-notification-context';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const i18n = inject(I18nService);
  const notification = inject(NotificationService);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        const resolved = resolveError(err, i18n);
        const enhancedError = err as HttpErrorResponse & {
          userMessage?: string;
          userMessageKey?: string;
        };
        enhancedError.userMessage = resolved.message;
        enhancedError.userMessageKey = resolved.messageKey;

        if (shouldShowErrorNotification(req.method, req.context.get(SKIP_ERROR_NOTIFICATION))) {
          const overrideKey = req.context.get(ERROR_MESSAGE_KEY);
          if (overrideKey) {
            notification.error(overrideKey);
          } else if (resolved.messageKey) {
            notification.error(resolved.messageKey);
          } else {
            notification.errorRaw(resolved.message);
          }
        }
      }
      return throwError(() => err);
    })
  );
};

function shouldShowErrorNotification(method: string, isSkipped: boolean): boolean {
  if (isSkipped) {
    return false;
  }
  if (method !== 'GET') {
    return true;
  }
  return false;
}

function resolveError(
  err: HttpErrorResponse,
  i18n: I18nService
): { message: string; messageKey?: string } {
  if (err.status === 0) {
    return { message: i18n.instant('notification.error.network'), messageKey: 'notification.error.network' };
  }
  if (err.status === 401) {
    if (err.url?.includes('/auth/login')) {
      return { message: i18n.instant('errors.badCredentials'), messageKey: 'errors.badCredentials' };
    }
    return { message: i18n.instant('notification.error.unauthorized'), messageKey: 'notification.error.unauthorized' };
  }
  if (err.status === 403) {
    const body = extractErrorBodyText(err);
    if (body && !isTechnicalJargon(body)) {
      return { message: body };
    }
    return { message: i18n.instant('notification.error.forbidden'), messageKey: 'notification.error.forbidden' };
  }
  if (err.status === 404) {
    const body = extractErrorBodyText(err);
    if (body && !isTechnicalJargon(body)) {
      return { message: body };
    }
    return { message: i18n.instant('notification.error.notFound'), messageKey: 'notification.error.notFound' };
  }
  if (err.status === 400) {
    if (err.error && typeof err.error === 'object' && !Array.isArray(err.error)) {
      const vals = Object.values(err.error as Record<string, string>);
      if (vals.length) {
        return { message: vals.join(' ') };
      }
    }
    const body = extractErrorBodyText(err);
    if (body && !isTechnicalJargon(body)) {
      return { message: body };
    }
  }
  if (err.status >= 500) {
    return { message: i18n.instant('notification.error.general'), messageKey: 'notification.error.general' };
  }
  const fallback = extractErrorBodyText(err);
  if (fallback && !isTechnicalJargon(fallback)) {
    return { message: fallback };
  }
  return { message: i18n.instant('notification.error.general'), messageKey: 'notification.error.general' };
}

function extractErrorBodyText(err: HttpErrorResponse): string | null {
  const payload = err.error;
  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim();
  }
  if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
    const record = payload as Record<string, unknown>;
    const message = record['message'];
    const error = record['error'];
    if (typeof message === 'string' && message.trim()) {
      return message.trim();
    }
    if (typeof error === 'string' && error.trim()) {
      return error.trim();
    }
  }
  return null;
}

function isTechnicalJargon(value: string): boolean {
  return /cors|csrf|xss|forbidden|unauthorized|nullpointer|stack trace|exception|internal server/i.test(
    value
  );
}
