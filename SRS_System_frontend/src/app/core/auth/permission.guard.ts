import { inject } from '@angular/core';
import { Router, type CanMatchFn } from '@angular/router';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { CapabilitiesService } from './capabilities.service';

/**
 * Blocks navigation when route {@link Route#data} contains {@code permission: 'CODE'} and the user lacks it.
 * Permission codes are defined in the database only.
 */
export const permissionCanMatch: CanMatchFn = (route, segments) => {
  const need = route.data?.['permission'] as string | undefined;
  if (!need) {
    return true;
  }
  const cap = inject(CapabilitiesService);
  const router = inject(Router);
  return cap.ensureReady().pipe(
    map(() => {
      if (cap.can(need)) {
        return true;
      }
      const deniedRoute = `/${segments.map((segment) => segment.path).join('/')}`;
      const target = cap.firstAllowedRoute(deniedRoute) ?? '/profile';
      return router.parseUrl(target);
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
