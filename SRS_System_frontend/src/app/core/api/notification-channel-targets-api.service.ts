import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface NotificationChannelTargetAdminDto {
  id: string;
  channelCode: string;
  targetCode: string;
  targetUrl: string | null;
  signingSecretRef: string | null;
  enabled: boolean;
  description: string | null;
}

export interface NotificationChannelTargetCreateDto {
  channelCode: string;
  targetCode: string;
  targetUrl?: string | null;
  signingSecretRef?: string | null;
  enabled: boolean;
  description?: string | null;
}

export interface NotificationChannelTargetUpdateDto {
  targetUrl?: string | null;
  signingSecretRef?: string | null;
  enabled?: boolean | null;
  description?: string | null;
}

/**
 * Slice 6 — admin CRUD for `/api/v1/notification-channel-targets`. Server-side rows store a
 * *reference* to the signing secret (env-var name), so the FE never accepts/displays the raw
 * value. The DTOs above mirror the backend records exactly.
 */
@Injectable({ providedIn: 'root' })
export class NotificationChannelTargetsApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  list(): Observable<NotificationChannelTargetAdminDto[]> {
    return this.http.get<NotificationChannelTargetAdminDto[]>(
      apiPath(this.base, AppConstants.API.NOTIFICATION_CHANNEL_TARGETS)
    );
  }

  create(body: NotificationChannelTargetCreateDto): Observable<NotificationChannelTargetAdminDto> {
    return this.http.post<NotificationChannelTargetAdminDto>(
      apiPath(this.base, AppConstants.API.NOTIFICATION_CHANNEL_TARGETS),
      body
    );
  }

  update(
    id: string,
    body: NotificationChannelTargetUpdateDto
  ): Observable<NotificationChannelTargetAdminDto> {
    return this.http.put<NotificationChannelTargetAdminDto>(
      apiPathWithId(this.base, AppConstants.API.NOTIFICATION_CHANNEL_TARGETS, id),
      body
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.NOTIFICATION_CHANNEL_TARGETS, id)
    );
  }
}
