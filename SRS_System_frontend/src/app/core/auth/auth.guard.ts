import { inject } from '@angular/core';
import { Router, type CanMatchFn } from '@angular/router';
import { AuthTokenService } from './auth-token.service';

/** Prevents the authenticated shell (including profile) from matching without a session token. */
export const authCanMatch: CanMatchFn = () => {
  const auth = inject(AuthTokenService);
  const router = inject(Router);
  return auth.getToken()?.trim() ? true : router.createUrlTree(['/login']);
};
