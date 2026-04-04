import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { NotificationItemDto, SpringPage } from './api-types';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(page = 0, size = 50): Observable<SpringPage<NotificationItemDto>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<SpringPage<NotificationItemDto>>(`${this.base}/notifications`, { params });
  }

  markRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/notifications/${id}/read`, {});
  }
}
