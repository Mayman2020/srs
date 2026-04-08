import { inject, Injectable } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';
import { API_BASE_URL } from '../../core/api/api-url';
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
  private readonly i18n = inject(I18nService);
  private readonly lang$ = toObservable(this.i18n.currentLang);

  /**
   * Hot observable — safe for `async` pipe or `toSignal(..., { initialValue: snapshot() })`.
   * Also recomputes when UI language changes (e.g. fallback display name uses `instant`).
   */
  readonly profile$: Observable<ErpUserProfileViewModel> = combineLatest([
    this.tokens.session$,
    this.lang$
  ]).pipe(
    map(([s]) => this.mapSession(s)),
    distinctUntilChanged((a, b) => a.rev === b.rev && a.displayName === b.displayName),
    shareReplay({ bufferSize: 1, refCount: false })
  );

  /** Synchronous snapshot for signal `initialValue` / guards. */
  snapshot(): ErpUserProfileViewModel {
    return this.mapSession(this.tokens.getSessionSnapshot());
  }

  private mapSession(s: AuthSessionSnapshot): ErpUserProfileViewModel {
    const displayName =
      s.username?.trim() || this.i18n.instant('topbar.demoUserName');
    const raw = s.profileImageUrl?.trim() ?? null;
    const resolved = raw ? this.resolveAssetUrl(raw) : null;
    const avatarPrimarySrc = resolved ? this.withCacheBuster(resolved, s.rev) : null;
    return {
      rev: s.rev,
      userId: s.userId,
      displayName,
      initials: this.initialsFrom(displayName),
      avatarPrimarySrc,
      currentRole: s.currentRole,
      roles: [...s.roles]
    };
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
