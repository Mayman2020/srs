import { HttpContext, HttpContextToken } from '@angular/common/http';

export const SUCCESS_MESSAGE_KEY = new HttpContextToken<string | null>(() => null);
export const ERROR_MESSAGE_KEY = new HttpContextToken<string | null>(() => null);
export const SKIP_SUCCESS_NOTIFICATION = new HttpContextToken<boolean>(() => false);
export const SKIP_ERROR_NOTIFICATION = new HttpContextToken<boolean>(() => false);

export function withSuccessMessage(
  messageKey: string,
  context: HttpContext = new HttpContext()
): HttpContext {
  return context.set(SUCCESS_MESSAGE_KEY, messageKey);
}

export function withErrorMessage(
  messageKey: string,
  context: HttpContext = new HttpContext()
): HttpContext {
  return context.set(ERROR_MESSAGE_KEY, messageKey);
}

export function withSilentSuccess(context: HttpContext = new HttpContext()): HttpContext {
  return context.set(SKIP_SUCCESS_NOTIFICATION, true);
}

export function withSilentError(context: HttpContext = new HttpContext()): HttpContext {
  return context.set(SKIP_ERROR_NOTIFICATION, true);
}

export function withSilentNotifications(context: HttpContext = new HttpContext()): HttpContext {
  return withSilentError(withSilentSuccess(context));
}
