import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

export interface EmailDispatchBody {
  to: string;
  subject: string;
  body: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationDispatchApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  dispatchEmail(body: EmailDispatchBody): Observable<void> {
    return this.http.post<void>(
      `${apiPath(this.base, AppConstants.API.NOTIFICATIONS)}/dispatch/email`,
      body
    );
  }
}
