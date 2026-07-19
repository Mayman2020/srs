import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenService } from '../auth/auth-token.service';

let refreshInFlight: ReturnType<AuthApiService['refresh']> | null = null;

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout') || url.includes('/auth/mfa');
}

/**
 * On 401, attempts a single in-flight refresh then retries the original request once.
 * Falls back to logout + login redirect when refresh is unavailable or fails.
 */
export const authRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const authApi = inject(AuthApiService);
  const tokens = inject(AuthTokenService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }
      if (isAuthEndpoint(req.url)) {
        return throwError(() => err);
      }
      const refreshToken = tokens.getRefreshToken();
      if (!refreshToken) {
        tokens.clear();
        void router.navigate(['/login']);
        return throwError(() => err);
      }
      if (!refreshInFlight) {
        refreshInFlight = authApi.refresh(refreshToken).pipe(
          catchError((refreshErr) => {
            refreshInFlight = null;
            tokens.clear();
            void router.navigate(['/login']);
            return throwError(() => refreshErr);
          })
        );
      }
      return refreshInFlight.pipe(
        switchMap((session) => {
          refreshInFlight = null;
          tokens.applySessionPayload({
            accessToken: session.accessToken,
            refreshToken: session.refreshToken ?? refreshToken,
            username: session.username,
            userId: session.userId,
            roles: session.roles,
            currentRole: session.currentRole
          });
          return next(
            req.clone({
              setHeaders: { Authorization: `Bearer ${session.accessToken}` }
            })
          );
        }),
        catchError(() => throwError(() => err))
      );
    })
  );
};
