import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export type NotificationOutboxStatus =
  | 'PENDING'
  | 'SENT'
  | 'FAILED'
  | 'DEAD'
  | 'CANCELLED'
  | '';

export interface NotificationOutboxAdminDto {
  id: string;
  idempotencyKey: string | null;
  eventTypeCode: string;
  channelCode: string;
  recipientUserId: string | null;
  recipientAddress: string | null;
  correlationResourceType?: string | null;
  correlationResourceId?: string | null;
  status: NotificationOutboxStatus;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastAttemptedAt: string | null;
  lastError: string | null;
}

/** Spring `Page<T>` projection used by the outbox admin endpoint. */
export interface NotificationOutboxPage {
  content: NotificationOutboxAdminDto[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/**
 * Slice 6 — admin endpoint for the notification outbox. Provides paged search/filter, requeue
 * (puts row back into PENDING with attempt=0/next_attempt_at=now), and cancel (DLQ-side
 * suppression).
 */
@Injectable({ providedIn: 'root' })
export class NotificationOutboxApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  page(
    status: NotificationOutboxStatus,
    page: number,
    size: number
  ): Observable<NotificationOutboxPage> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<NotificationOutboxPage>(
      apiPath(this.base, AppConstants.API.NOTIFICATION_OUTBOX_ADMIN),
      { params }
    );
  }

  requeue(id: string): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.NOTIFICATION_OUTBOX_ADMIN, id)}/requeue`,
      {}
    );
  }

  cancel(id: string): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.NOTIFICATION_OUTBOX_ADMIN, id)}/cancel`,
      {}
    );
  }
}
