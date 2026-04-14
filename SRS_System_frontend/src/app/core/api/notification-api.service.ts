import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { NotificationItemDto, SpringPage } from './api-types';
import { withSilentSuccess } from '../interceptors/http-notification-context';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(page = 0, size = 50): Observable<SpringPage<NotificationItemDto>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .append('sort', 'createdAt,desc');
    return this.http.get<SpringPage<NotificationItemDto>>(
      apiPath(this.base, AppConstants.API.NOTIFICATIONS),
      { params }
    );
  }

  markRead(id: string): Observable<void> {
    return this.http.patch<void>(`${apiPathWithId(this.base, AppConstants.API.NOTIFICATIONS, id)}/read`, {}, {
      context: withSilentSuccess()
    });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(apiPathWithId(this.base, AppConstants.API.NOTIFICATIONS, id));
  }
}
