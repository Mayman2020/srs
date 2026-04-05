import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LoginResponseDto } from './api-types';
import { AuthTokenService } from '../auth/auth-token.service';

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
    return this.http.post<LoginResponseDto>(`${this.base}/auth/login`, body).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole
        });
      })
    );
  }

  mfaChallenge(username: string, channel: 'EMAIL' | 'SMS'): Observable<void> {
    return this.http.post<void>(`${this.base}/auth/mfa/challenge`, { username, channel });
  }

  mfaVerify(body: MfaVerifyRequestDto): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>(`${this.base}/auth/mfa/verify`, body).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole
        });
      })
    );
  }

  refresh(refreshToken: string): Observable<LoginResponseDto> {
    const body: RefreshRequestDto = { refreshToken };
    return this.http.post<LoginResponseDto>(`${this.base}/auth/refresh`, body).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? refreshToken,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole
        });
      })
    );
  }

  switchRole(roleCode: string): Observable<LoginResponseDto> {
    const body: SwitchRoleRequestDto = { roleCode };
    return this.http.post<LoginResponseDto>(`${this.base}/auth/switch-role`, body).pipe(
      tap((r) => {
        this.tokens.applySessionPayload({
          accessToken: r.accessToken,
          refreshToken: r.refreshToken ?? undefined,
          username: r.username,
          userId: r.userId,
          roles: r.roles,
          currentRole: r.currentRole
        });
      })
    );
  }

  logout(): void {
    this.tokens.clear();
  }
}
