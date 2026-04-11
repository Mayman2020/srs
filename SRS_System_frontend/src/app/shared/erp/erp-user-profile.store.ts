import { inject, Injectable } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';
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

  /**
   * Hot observable — safe for `async` pipe or `toSignal(..., { initialValue: snapshot() })`.
   * Also recomputes when UI language changes (e.g. fallback display name uses `instant`).
   */
  readonly profile$: Observable<ErpUserProfileViewModel> = combineLatest([
    this.tokens.session$,
    this.currentProfileApi.currentProfile$,
    this.lang$
  ]).pipe(
    map(([s, profile]) => this.mapSession(s, profile)),
    distinctUntilChanged(
      (a, b) =>
        a.rev === b.rev &&
        a.displayName === b.displayName &&
        a.avatarPrimarySrc === b.avatarPrimarySrc &&
        a.currentRole === b.currentRole
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
      } | null): ErpUserProfileViewModel {
    const displayName = this.resolveDisplayName(s, profile);
    const raw = profile?.profileImageUrl?.trim() || s.profileImageUrl?.trim() || null;
    const resolved = raw ? this.resolveAssetUrl(raw) : null;
    const avatarPrimarySrc = resolved ? this.withCacheBuster(resolved, s.rev) : null;
    return {
      rev: s.rev,
      userId: s.userId,
      displayName,
      initials: this.initialsFrom(displayName),
      avatarPrimarySrc,
      currentRole: s.currentRole,
      roles: [...s.roles],
      lastLoginAt: profile?.lastLoginAt ?? null
    };
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
}
