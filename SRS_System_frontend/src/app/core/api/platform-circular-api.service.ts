import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';

export interface PlatformCircularInboxRow {
  id: string;
  title: string;
  createdBy: string;
  createdAt: string;
  broadcast: boolean;
  read: boolean;
}

export interface PlatformCreateCircularRequest {
  title: string;
  body: string;
  createdBy: string;
  broadcast: boolean;
  recipientUserIds: string[];
}

export interface PlatformMarkCircularReadRequest {
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformCircularApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  inbox(userId: string): Observable<PlatformCircularInboxRow[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<PlatformCircularInboxRow[]>(`${this.base}/circulars/inbox`, { params });
  }

  markRead(circularId: string, body: PlatformMarkCircularReadRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/circulars/${circularId}/read`, body);
  }

  create(body: PlatformCreateCircularRequest): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`${this.base}/circulars`, body);
  }
}
