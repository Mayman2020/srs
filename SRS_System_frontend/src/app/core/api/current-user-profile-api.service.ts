import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';
import { API_BASE_URL } from './api-url';
import { CurrentUserProfileDto } from './api-types';
import { AuthTokenService } from '../auth/auth-token.service';
import { I18nService, AppLang } from '../i18n/i18n.service';
import { ThemeService } from '../theme/theme.service';
import { withSilentNotifications } from '../interceptors/http-notification-context';

@Injectable({ providedIn: 'root' })
export class CurrentUserProfileApiService {
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private readonly i18n = inject(I18nService);
  private readonly theme = inject(ThemeService);

  readonly currentProfile$: Observable<CurrentUserProfileDto | null> = this.reload$.pipe(
    switchMap(() => this.tokens.session$),
    switchMap((session) => {
      if (!session.userId?.trim()) {
        return of(null);
      }
      return this.http.get<CurrentUserProfileDto>(`${this.base}/profile/me`).pipe(
        tap((dto) => this.applyServerUiPreferences(dto)),
        catchError((err: unknown) => {
          console.error('[CurrentUserProfileApi] profile load failed', err);
          return of(null);
        })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string,
    private tokens: AuthTokenService
  ) {}

  getMyProfile(): Observable<CurrentUserProfileDto> {
    return this.http.get<CurrentUserProfileDto>(`${this.base}/profile/me`);
  }

  updateMyProfile(body: {
    fullNameAr: string;
    fullNameEn: string;
    email: string;
    phone?: string | null;
    nationalId?: string | null;
  }): Observable<CurrentUserProfileDto> {
    return this.http
      .put<CurrentUserProfileDto>(`${this.base}/profile/me`, body)
      .pipe(tap(() => this.refresh()));
  }

  uploadMyAvatar(file: File): Observable<CurrentUserProfileDto> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .put<CurrentUserProfileDto>(`${this.base}/profile/me/avatar`, formData)
      .pipe(tap(() => this.refresh()));
  }

  getMyAvatarBlob(): Observable<Blob> {
    return this.http.get(`${this.base}/profile/me/avatar`, {
      responseType: 'blob',
      context: withSilentNotifications()
    });
  }

  updateMyPassword(body: {
    currentPassword: string;
    newPassword: string;
  }): Observable<void> {
    return this.http.put<void>(`${this.base}/profile/me/password`, body);
  }

  updateMyUiPreferences(body: {
    uiTheme: 'light' | 'dark';
    uiLocale: AppLang;
  }): Observable<CurrentUserProfileDto> {
    return this.http.put<CurrentUserProfileDto>(`${this.base}/profile/me/ui`, body, {
      context: withSilentNotifications()
    }).pipe(
      tap((dto) => this.applyServerUiPreferences(dto)),
      tap(() => this.refresh())
    );
  }

  refresh(): void {
    this.reload$.next();
  }

  private applyServerUiPreferences(dto: CurrentUserProfileDto | null): void {
    if (!dto) {
      return;
    }
    if (dto.uiTheme === 'light' || dto.uiTheme === 'dark') {
      this.theme.syncFromServer(dto.uiTheme);
    }
    const loc = dto.uiLocale;
    if (loc === 'ar' || loc === 'en') {
      if (loc !== this.i18n.currentLang()) {
        this.i18n.loadLang(loc).subscribe({ error: () => {} });
      }
    }
  }
}
