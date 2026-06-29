import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthTokenService } from '../auth/auth-token.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokens = inject(AuthTokenService);
  if (isPublicAuthRequest(req.url)) {
    return next(req);
  }
  const token = tokens.getToken();
  if (!token || req.headers.has('Authorization')) {
    return next(req);
  }
  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    })
  );
};

function isPublicAuthRequest(url: string): boolean {
  return (
    url.includes('/auth/login') ||
    url.includes('/auth/refresh') ||
    url.includes('/auth/mfa') ||
    url.includes('/auth/forgot-password') ||
    url.includes('/auth/reset-password')
  );
}
