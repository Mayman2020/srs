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
        this.tokens.setToken(r.accessToken);
        this.tokens.setUsername(r.username ?? null);
      })
    );
  }

  logout(): void {
    this.tokens.clear();
  }
}
