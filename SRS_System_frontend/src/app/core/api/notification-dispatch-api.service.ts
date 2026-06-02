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

export interface SmsDispatchBody {
  phoneE164: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationDispatchApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  dispatchEmail(body: EmailDispatchBody): Observable<void> {
    return this.http.post<void>(
      `${apiPath(this.base, AppConstants.API.NOTIFICATION_DISPATCH)}/email`,
      body
    );
  }

  dispatchSms(body: SmsDispatchBody): Observable<void> {
    return this.http.post<void>(
      `${apiPath(this.base, AppConstants.API.NOTIFICATION_DISPATCH)}/sms`,
      body
    );
  }
}
