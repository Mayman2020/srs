import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import type { AuthSessionSnapshot } from './auth-session.types';

const STORAGE_KEY = 'ac_access_token';
const REFRESH_TOKEN_KEY = 'ac_refresh_token';
const USERNAME_KEY = 'ac_username';
const USER_ID_KEY = 'ac_user_id';
const ROLES_KEY = 'ac_roles_json';
const CURRENT_ROLE_KEY = 'ac_current_role';
const AVATAR_URL_KEY = 'ac_avatar_url';

export interface AuthSessionPayload {
  accessToken: string;
  refreshToken?: string | null;
  username?: string | null;
  userId?: string | null;
  roles?: string[] | null;
  currentRole?: string | null;
  profileImageUrl?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  private rev = 0;
  private readonly _session = new BehaviorSubject<AuthSessionSnapshot>(this.buildSnapshot());

  /**
   * Reactive session snapshot for shell UI. Emits on login, token refresh, switch-role, and logout.
   * Prefer `toSignal(this.session$, { initialValue: this.getSessionSnapshot() })` or `async` pipe.
   */
  readonly session$ = this._session.asObservable();

  /**
   * @deprecated Use {@link session$} or {@link getSessionSnapshot}. Kept for compatibility; this is
   * the same stream as `session$` (not a bare revision counter).
   */
  readonly sessionChanged$ = this.session$;

  getSessionSnapshot(): AuthSessionSnapshot {
    return this._session.value;
  }

  getToken(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }

  setToken(token: string): void {
    try {
      localStorage.setItem(STORAGE_KEY, token);
    } catch {
      /* ignore */
    }
  }

  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_TOKEN_KEY);
    } catch {
      return null;
    }
  }

  setRefreshToken(token: string | null): void {
    try {
      if (token) {
        localStorage.setItem(REFRESH_TOKEN_KEY, token);
      } else {
        localStorage.removeItem(REFRESH_TOKEN_KEY);
      }
    } catch {
      /* ignore */
    }
  }

  getUsername(): string | null {
    try {
      return localStorage.getItem(USERNAME_KEY);
    } catch {
      return null;
    }
  }

  setUsername(username: string | null): void {
    try {
      if (username) {
        localStorage.setItem(USERNAME_KEY, username);
      } else {
        localStorage.removeItem(USERNAME_KEY);
      }
    } catch {
      /* ignore */
    }
  }

  getUserId(): string | null {
    try {
      return localStorage.getItem(USER_ID_KEY);
    } catch {
      return null;
    }
  }

  getRoles(): string[] {
    try {
      const raw = localStorage.getItem(ROLES_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as unknown;
        if (Array.isArray(parsed)) {
          return parsed.map((x) => String(x));
        }
      }
    } catch {
      /* ignore */
    }
    const fromJwt = this.readRolesFromToken(this.getToken());
    return fromJwt ?? [];
  }

  getCurrentRole(): string | null {
    try {
      const r = localStorage.getItem(CURRENT_ROLE_KEY);
      if (r?.trim()) {
        return r.trim();
      }
    } catch {
      /* ignore */
    }
    return this.readCurrentRoleFromToken(this.getToken());
  }

  getProfileImageUrl(): string | null {
    try {
      return localStorage.getItem(AVATAR_URL_KEY);
    } catch {
      return null;
    }
  }

  /** Persists token, identity, roles, and optional avatar from login or switch-role. */
  applySessionPayload(payload: AuthSessionPayload): void {
    this.setToken(payload.accessToken);
    if (payload.refreshToken !== undefined) {
      this.setRefreshToken(payload.refreshToken ?? null);
    }
    if (payload.username !== undefined) {
      this.setUsername(payload.username ?? null);
    }
    try {
      if (payload.userId) {
        localStorage.setItem(USER_ID_KEY, payload.userId);
      } else {
        const fromJwt = this.readUserIdFromToken(payload.accessToken);
        if (fromJwt) {
          localStorage.setItem(USER_ID_KEY, fromJwt);
        } else {
          localStorage.removeItem(USER_ID_KEY);
        }
      }
    } catch {
      /* ignore */
    }
    const roles =
      payload.roles && payload.roles.length
        ? payload.roles
        : this.readRolesFromToken(payload.accessToken) ?? [];
    try {
      localStorage.setItem(ROLES_KEY, JSON.stringify(roles));
    } catch {
      /* ignore */
    }
    const current =
      payload.currentRole?.trim() ||
      this.readCurrentRoleFromToken(payload.accessToken) ||
      (roles.length ? roles[0] : null);
    try {
      if (current) {
        localStorage.setItem(CURRENT_ROLE_KEY, current);
      } else {
        localStorage.removeItem(CURRENT_ROLE_KEY);
      }
    } catch {
      /* ignore */
    }
    this.syncAvatarStorage(payload);
    this.pushSession();
  }

  clear(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(USER_ID_KEY);
      localStorage.removeItem(ROLES_KEY);
      localStorage.removeItem(CURRENT_ROLE_KEY);
      localStorage.removeItem(AVATAR_URL_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    } catch {
      /* ignore */
    }
    this.pushSession();
  }

  private syncAvatarStorage(payload: AuthSessionPayload): void {
    try {
      if (payload.profileImageUrl !== undefined) {
        if (payload.profileImageUrl) {
          localStorage.setItem(AVATAR_URL_KEY, payload.profileImageUrl);
        } else {
          localStorage.removeItem(AVATAR_URL_KEY);
        }
        return;
      }
      const fromJwt = this.readProfileImageUrlFromToken(payload.accessToken);
      if (fromJwt) {
        localStorage.setItem(AVATAR_URL_KEY, fromJwt);
      }
    } catch {
      /* ignore */
    }
  }

  private pushSession(): void {
    this.rev += 1;
    this._session.next(this.buildSnapshot());
  }

  private buildSnapshot(): AuthSessionSnapshot {
    return {
      rev: this.rev,
      username: this.getUsername(),
      userId: this.getUserId(),
      profileImageUrl: this.getProfileImageUrl(),
      roles: [...this.getRoles()],
      currentRole: this.getCurrentRole()
    };
  }

  private readProfileImageUrlFromToken(token: string | null): string | null {
    const p = this.decodeJwtPayload(token);
    if (!p) {
      return null;
    }
    const keys = ['picture', 'profileImageUrl', 'avatar_url', 'avatar'] as const;
    for (const k of keys) {
      const v = p[k];
      if (typeof v === 'string' && v.trim()) {
        return v.trim();
      }
    }
    return null;
  }

  private readUserIdFromToken(token: string | null): string | null {
    const p = this.decodeJwtPayload(token);
    const sub = p && typeof p['sub'] === 'string' ? (p['sub'] as string).trim() : '';
    const uid = p && typeof p['userId'] === 'string' ? (p['userId'] as string).trim() : '';
    return uid || sub || null;
  }

  private readRolesFromToken(token: string | null): string[] | null {
    const p = this.decodeJwtPayload(token);
    const raw = p?.['roles'];
    if (!Array.isArray(raw)) {
      return null;
    }
    return raw.map((x) => String(x));
  }

  private readCurrentRoleFromToken(token: string | null): string | null {
    const p = this.decodeJwtPayload(token);
    const cr =
      p && typeof p['currentRole'] === 'string' ? (p['currentRole'] as string).trim() : '';
    const ar =
      p && typeof p['active_role'] === 'string' ? (p['active_role'] as string).trim() : '';
    return cr || ar || null;
  }

  private decodeJwtPayload(token: string | null): Record<string, unknown> | null {
    if (!token) {
      return null;
    }
    try {
      const parts = token.split('.');
      if (parts.length < 2) {
        return null;
      }
      const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
      const json = decodeURIComponent(
        atob(padded)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(json) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
