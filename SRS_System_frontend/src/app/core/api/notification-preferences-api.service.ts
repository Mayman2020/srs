import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

export interface NotificationPreferenceRowDto {
  id: string;
  eventTypeCode: string;
  channelCode: string;
  enabled: boolean;
}

export interface NotificationPreferenceUpsertDto {
  eventTypeCode: string;
  channelCode: string;
  enabled: boolean;
}

/**
 * Slice 6 — per-user notification preferences. Calls `/api/v1/me/notification-preferences`.
 * Reads/writes are gated server-side by `NOTIFICATION_PREFERENCE_MANAGE`; the route guard sets
 * the same permission so the screen is hidden for users who cannot manage their own preferences.
 */
@Injectable({ providedIn: 'root' })
export class NotificationPreferencesApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  list(): Observable<NotificationPreferenceRowDto[]> {
    return this.http.get<NotificationPreferenceRowDto[]>(
      apiPath(this.base, AppConstants.API.NOTIFICATION_PREFERENCES_ME)
    );
  }

  replace(rows: NotificationPreferenceUpsertDto[]): Observable<void> {
    return this.http.put<void>(
      apiPath(this.base, AppConstants.API.NOTIFICATION_PREFERENCES_ME),
      rows
    );
  }
}
