import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, throwError, of } from 'rxjs';
import { catchError, map, shareReplay, takeUntil, tap } from 'rxjs/operators';
import { API_BASE_URL } from '../api/api-url';
import type { UserCapabilitiesDto } from '../api/api-types';
import { AuthTokenService } from './auth-token.service';
import { AppConstants, apiPath } from '../constants/app-constants';

/**
 * Loads capabilities after login and exposes {@link #can} for UI and guards.
 * Permissions are never hardcoded as business rules — only compared to codes returned by the API.
 */
@Injectable({ providedIn: 'root' })
export class CapabilitiesService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private permissionCodes = new Set<string>();
  private snapshot: UserCapabilitiesDto | null = null;
  private cachedLoad: Observable<UserCapabilitiesDto> | null = null;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string,
    private auth: AuthTokenService
  ) {
    this.auth.session$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      const t = this.auth.getToken()?.trim();
      if (!t) {
        this.clear();
        return;
      }
      this.snapshot = null;
      this.permissionCodes.clear();
      this.cachedLoad = null;
      this.load().subscribe({ error: () => undefined });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Whether the current API snapshot includes this permission code (as returned by the server). */
  can(permissionCode: string): boolean {
    return this.permissionCodes.has(permissionCode);
  }

  /**
   * True when at least one of the given canonical permission codes is in the user's effective
   * union. Templates can short-circuit with this; the backend remains the source of truth for
   * authorization.
   */
  canAny(permissionCodes: readonly string[]): boolean {
    if (!permissionCodes || permissionCodes.length === 0) {
      return false;
    }
    for (const code of permissionCodes) {
      if (this.permissionCodes.has(code)) {
        return true;
      }
    }
    return false;
  }

  /**
   * True when every given canonical permission code is in the user's effective union. Returns
   * false for an empty list to avoid accidental "always true" gates in templates.
   */
  canAll(permissionCodes: readonly string[]): boolean {
    if (!permissionCodes || permissionCodes.length === 0) {
      return false;
    }
    for (const code of permissionCodes) {
      if (!this.permissionCodes.has(code)) {
        return false;
      }
    }
    return true;
  }

  getSnapshot(): UserCapabilitiesDto | null {
    return this.snapshot;
  }

  clear(): void {
    this.snapshot = null;
    this.permissionCodes.clear();
    this.cachedLoad = null;
  }

  /**
   * Ensures capabilities are loaded for the current session. Used by route guards.
   */
  ensureReady(): Observable<void> {
    const token = this.auth.getToken()?.trim();
    if (!token) {
      return throwError(() => new Error('Not authenticated'));
    }
    if (this.snapshot) {
      return of(undefined);
    }
    return this.load().pipe(map(() => undefined));
  }

  load(): Observable<UserCapabilitiesDto> {
    const token = this.auth.getToken()?.trim();
    if (!token) {
      this.clear();
      return throwError(() => new Error('Not authenticated'));
    }
    if (this.cachedLoad) {
      return this.cachedLoad;
    }
    this.cachedLoad = this.http.get<UserCapabilitiesDto>(
      apiPath(this.base, AppConstants.API.ME_CAPABILITIES)
    ).pipe(
      tap((dto) => {
        this.snapshot = dto;
        this.permissionCodes = new Set(dto.permissions ?? []);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
      catchError((err) => {
        this.cachedLoad = null;
        return throwError(() => err);
      })
    );
    return this.cachedLoad;
  }
}
