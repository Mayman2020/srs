import { inject, Injectable } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, map, shareReplay, switchMap } from 'rxjs/operators';
import { API_BASE_URL } from '../../core/api/api-url';
import { CurrentUserProfileApiService } from '../../core/api/current-user-profile-api.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import type { AuthSessionSnapshot } from '../../core/auth/auth-session.types';
import { I18nService } from '../../core/i18n/i18n.service';
import type { ErpUserProfileViewModel } from './erp-user-profile.types';

/** Default silhouette when the user has no photo or the image fails to load (under `public/`). */
export const ERP_AVATAR_FALLBACK_ASSET = 'assets/img/avatar-placeholder.svg';

/**
 * Single source of truth for user display in header, sidebar, and future settings screens.
 * Derives from {@link AuthTokenService.session$} with resolved image URLs and i18n display name.
 */
@Injectable({ providedIn: 'root' })
export class ErpUserProfileStore {
  private readonly tokens = inject(AuthTokenService);
  private readonly apiBase = inject(API_BASE_URL);
  private readonly currentProfileApi = inject(CurrentUserProfileApiService);
  private readonly i18n = inject(I18nService);
  private readonly lang$ = toObservable(this.i18n.currentLang);
  private protectedAvatarObjectUrl: string | null = null;
  private protectedAvatarKey: string | null = null;

  /**
   * Hot observable — safe for `async` pipe or `toSignal(..., { initialValue: snapshot() })`.
   * Also recomputes when UI language changes (e.g. fallback display name uses `instant`).
   */
  readonly profile$: Observable<ErpUserProfileViewModel> = combineLatest([
    this.tokens.session$,
    this.currentProfileApi.currentProfile$,
    this.lang$
  ]).pipe(
    switchMap(([s, profile]) =>
      this.resolveAvatarPrimarySrc(s, profile).pipe(
        map((avatarPrimarySrc) => this.mapSession(s, profile, avatarPrimarySrc))
      )
    ),
    distinctUntilChanged(
      (a, b) =>
        a.rev === b.rev &&
        a.displayName === b.displayName &&
        a.avatarPrimarySrc === b.avatarPrimarySrc &&
        a.currentRole === b.currentRole &&
        this.sameRoles(a.roles, b.roles)
    ),
    shareReplay({ bufferSize: 1, refCount: false })
  );

  /** Synchronous snapshot for signal `initialValue` / guards. */
  snapshot(): ErpUserProfileViewModel {
    return this.mapSession(this.tokens.getSessionSnapshot(), null);
  }

  private mapSession(
      s: AuthSessionSnapshot,
      profile: {
        fullNameAr: string;
        fullNameEn: string;
        profileImageUrl?: string | null;
        lastLoginAt: string | null;
        roleCodes?: readonly string[] | null;
      } | null,
      avatarPrimarySrc: string | null = this.resolveFallbackAvatarSrc(s, profile)): ErpUserProfileViewModel {
    const displayName = this.resolveDisplayName(s, profile);
    return {
      rev: s.rev,
      userId: s.userId,
      displayName,
      initials: this.initialsFrom(displayName),
      avatarPrimarySrc,
      currentRole: s.currentRole,
      roles: this.resolveRoles(s, profile),
      lastLoginAt: profile?.lastLoginAt ?? null
    };
  }

  private resolveAvatarPrimarySrc(
    session: AuthSessionSnapshot,
    profile: {
      profileImageUrl?: string | null;
    } | null
  ): Observable<string | null> {
    const raw = profile?.profileImageUrl?.trim() || session.profileImageUrl?.trim() || null;
    if (!raw) {
      this.clearProtectedAvatarCache();
      return of(null);
    }

    const resolved = this.resolveAssetUrl(raw);
    if (!this.isProtectedMyAvatarUrl(resolved)) {
      this.clearProtectedAvatarCache();
      return of(this.withCacheBuster(resolved, session.rev));
    }

    const key = `${session.userId ?? ''}|${raw}`;
    if (this.protectedAvatarKey === key && this.protectedAvatarObjectUrl) {
      return of(this.protectedAvatarObjectUrl);
    }

    return this.currentProfileApi.getMyAvatarBlob().pipe(
      map((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.cacheProtectedAvatar(key, objectUrl);
        return objectUrl;
      }),
      catchError(() => {
        this.clearProtectedAvatarCache();
        return of(null);
      })
    );
  }

  private resolveDisplayName(
    session: AuthSessionSnapshot,
    profile: { fullNameAr: string; fullNameEn: string } | null
  ): string {
    const lang = this.i18n.currentLang();
    const fromProfile =
      lang === 'ar' ? profile?.fullNameAr?.trim() : profile?.fullNameEn?.trim();
    return (
      fromProfile ||
      session.username?.trim() ||
      this.i18n.instant('topbar.demoUserName')
    );
  }

  private initialsFrom(name: string): string {
    const t = name.trim();
    if (!t.length) {
      return '?';
    }
    return t.charAt(0).toUpperCase();
  }

  private resolveRoles(
    session: AuthSessionSnapshot,
    profile: { roleCodes?: readonly string[] | null } | null
  ): string[] {
    const rawCodes = [...(profile?.roleCodes ?? []), ...session.roles, session.currentRole];
    const seen = new Set<string>();
    const roles: string[] = [];

    for (const raw of rawCodes) {
      const code = String(raw ?? '').trim();
      if (!code || seen.has(code)) {
        continue;
      }
      seen.add(code);
      roles.push(code);
    }

    return roles;
  }

  private sameRoles(a: readonly string[], b: readonly string[]): boolean {
    if (a.length !== b.length) {
      return false;
    }
    return a.every((role, index) => role === b[index]);
  }

  private resolveFallbackAvatarSrc(
    session: AuthSessionSnapshot,
    profile: {
      profileImageUrl?: string | null;
    } | null
  ): string | null {
    const raw = profile?.profileImageUrl?.trim() || session.profileImageUrl?.trim() || null;
    if (!raw) {
      return null;
    }
    const resolved = this.resolveAssetUrl(raw);
    return this.withCacheBuster(resolved, session.rev);
  }

  /** Turn API-relative paths into same-origin URLs suitable for `<img src>`. */
  private resolveAssetUrl(raw: string): string {
    const t = raw.trim();
    if (/^https?:\/\//i.test(t) || t.startsWith('data:')) {
      return t;
    }
    if (t.startsWith('/')) {
      return t;
    }
    const base = this.apiBase.replace(/\/$/, '');
    return `${base}/${t.replace(/^\/+/, '')}`;
  }

  private withCacheBuster(url: string, rev: number): string {
    const sep = url.includes('?') ? '&' : '?';
    return `${url}${sep}_s=${rev}`;
  }

  private isProtectedMyAvatarUrl(url: string): boolean {
    return /\/api\/v1\/profile\/me\/avatar(?:\?|$)/.test(url);
  }

  private cacheProtectedAvatar(key: string, url: string): void {
    if (this.protectedAvatarObjectUrl && this.protectedAvatarObjectUrl !== url) {
      URL.revokeObjectURL(this.protectedAvatarObjectUrl);
    }
    this.protectedAvatarKey = key;
    this.protectedAvatarObjectUrl = url;
  }

  private clearProtectedAvatarCache(): void {
    if (this.protectedAvatarObjectUrl) {
      URL.revokeObjectURL(this.protectedAvatarObjectUrl);
    }
    this.protectedAvatarObjectUrl = null;
    this.protectedAvatarKey = null;
  }
}
