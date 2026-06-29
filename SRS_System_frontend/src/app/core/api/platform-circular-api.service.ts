import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface PlatformCircularInboxRowDto {
  id: string;
  title: string;
  createdBy: string;
  createdAt: string;
  broadcast: boolean;
  read: boolean;
}

export interface PlatformCreateCircularRequestDto {
  title: string;
  body: string;
  /**
   * Server overrides this from the JWT subject; we send the current user id only so existing
   * payload validation passes.
   */
  createdBy: string;
  broadcast: boolean;
  recipientUserIds: string[];
}

@Injectable({ providedIn: 'root' })
export class PlatformCircularApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  /** Self-scoped — recipient is always the JWT subject; no userId parameter. */
  inbox(): Observable<PlatformCircularInboxRowDto[]> {
    return this.http.get<PlatformCircularInboxRowDto[]>(
      `${apiPath(this.base, AppConstants.API.CIRCULARS)}/inbox`
    );
  }

  /** Mark as read for the current user; backend derives user id from JWT. */
  markRead(circularId: string): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.CIRCULARS, circularId)}/read`,
      {}
    );
  }

  create(body: PlatformCreateCircularRequestDto): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(
      apiPath(this.base, AppConstants.API.CIRCULARS),
      body
    );
  }

  broadcast(body: PlatformCreateCircularRequestDto): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(
      `${apiPath(this.base, AppConstants.API.CIRCULARS)}/broadcast`,
      body
    );
  }

  readReport(): Observable<CircularReadReportSummaryDto[]> {
    return this.http.get<CircularReadReportSummaryDto[]>(
      `${apiPath(this.base, AppConstants.API.CIRCULARS)}/read-report`
    );
  }

  readStatus(circularId: string): Observable<CircularReadStatusDto> {
    return this.http.get<CircularReadStatusDto>(
      `${apiPathWithId(this.base, AppConstants.API.CIRCULARS, circularId)}/read-status`
    );
  }
}

export interface CircularReadReportSummaryDto {
  circularId: string;
  title: string;
  broadcast: boolean;
  createdAt: string;
  totalRecipients: number;
  readCount: number;
  readPercent: number;
}

export interface CircularReadStatusDto extends CircularReadReportSummaryDto {
  recipients: CircularReadRecipientRowDto[];
}

export interface CircularReadRecipientRowDto {
  userId: string;
  username: string | null;
  fullNameAr: string | null;
  fullNameEn: string | null;
  readAt: string | null;
  read: boolean;
}
