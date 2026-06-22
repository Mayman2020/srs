import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LoginResponseDto } from './api-types';
import { AuthTokenService } from '../auth/auth-token.service';
import { withSilentNotifications, withSilentSuccess } from '../interceptors/http-notification-context';
import { AppConstants, apiPath } from '../constants/app-constants';

export interface LoginRequestDto {
  username: string;
  password: string;
}

export interface SwitchRoleRequestDto {
  roleCode: string;
}

export interface RefreshRequestDto {
  refreshToken: string;
}

export interface MfaVerifyRequestDto {
  username: string;
  password: string;
  code: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string,
    private tokens: AuthTokenService
  ) {}

  login(body: LoginRequestDto): Observable<LoginResponseDto> {
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<LoginResponseDto>(`${auth}/login`, body, {
      context: withSilentNotifications()
    }).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole,
          profileImageUrl: r.profileImageUrl
        });
      })
    );
  }

  mfaChallenge(username: string, channel: 'EMAIL' | 'SMS'): Observable<void> {
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<void>(`${auth}/mfa/challenge`, { username, channel }, {
      context: withSilentNotifications()
    });
  }

  mfaVerify(body: MfaVerifyRequestDto): Observable<LoginResponseDto> {
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<LoginResponseDto>(`${auth}/mfa/verify`, body, {
      context: withSilentNotifications()
    }).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole,
          profileImageUrl: r.profileImageUrl
        });
      })
    );
  }

  refresh(refreshToken: string): Observable<LoginResponseDto> {
    const body: RefreshRequestDto = { refreshToken };
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<LoginResponseDto>(`${auth}/refresh`, body, {
      context: withSilentNotifications()
    }).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? refreshToken,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole,
          profileImageUrl: r.profileImageUrl
        });
      })
    );
  }

  switchRole(roleCode: string): Observable<LoginResponseDto> {
    const body: SwitchRoleRequestDto = { roleCode };
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<LoginResponseDto>(`${auth}/switch-role`, body, {
      context: withSilentSuccess()
    }).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole,
          profileImageUrl: r.profileImageUrl
        });
      })
    );
  }

  logout(): void {
    this.tokens.clear();
  }

  forgotPassword(username: string): Observable<void> {
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<void>(`${auth}/forgot-password`, { username }, {
      context: withSilentNotifications()
    });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    const auth = apiPath(this.base, AppConstants.API.AUTH);
    return this.http.post<void>(`${auth}/reset-password`, { token, newPassword }, {
      context: withSilentNotifications()
    });
  }
}
