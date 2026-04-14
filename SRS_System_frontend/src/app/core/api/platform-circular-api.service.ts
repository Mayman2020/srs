import { HttpClient, HttpParams } from '@angular/common/http';
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
  createdBy: string;
  broadcast: boolean;
  recipientUserIds: string[];
}

export interface PlatformMarkCircularReadRequestDto {
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformCircularApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  inbox(userId: string): Observable<PlatformCircularInboxRowDto[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<PlatformCircularInboxRowDto[]>(
      `${apiPath(this.base, AppConstants.API.CIRCULARS)}/inbox`,
      { params }
    );
  }

  markRead(circularId: string, body: PlatformMarkCircularReadRequestDto): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.CIRCULARS, circularId)}/read`,
      body
    );
  }

  create(body: PlatformCreateCircularRequestDto): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(
      apiPath(this.base, AppConstants.API.CIRCULARS),
      body
    );
  }
}
