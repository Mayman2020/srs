import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

export interface NotificationCatalogItemDto {
  code: string;
  nameEn: string;
  nameAr: string;
}

export interface NotificationCatalogDto {
  eventTypes: NotificationCatalogItemDto[];
  channels: NotificationCatalogItemDto[];
}

/**
 * Slice 6 — read-only catalog of notification event types and channels for preference + admin
 * UIs. Cached for the session so opening multiple admin tabs does not re-fetch the same metadata.
 */
@Injectable({ providedIn: 'root' })
export class NotificationCatalogApiService {
  private cache$?: Observable<NotificationCatalogDto>;

  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  load(): Observable<NotificationCatalogDto> {
    if (!this.cache$) {
      this.cache$ = this.http
        .get<NotificationCatalogDto>(apiPath(this.base, AppConstants.API.NOTIFICATION_CATALOG))
        .pipe(shareReplay(1));
    }
    return this.cache$;
  }

  invalidate(): void {
    this.cache$ = undefined;
  }
}
