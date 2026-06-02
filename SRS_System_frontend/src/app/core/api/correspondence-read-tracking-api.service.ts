import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppConstants, apiPathWithId } from '../constants/app-constants';
import { API_BASE_URL } from './api-url';
import {
  CorrespondenceAckRequestDto,
  CorrespondenceReadReceiptDto,
  CorrespondenceReadStatusSummaryDto
} from './api-types';

/**
 * Slice 1 — defense-grade hardening: read receipt acknowledgement + cross-user read status.
 *
 * Endpoints:
 * - `POST /api/v1/correspondence/{id}/ack` — open to any authenticated viewer.
 * - `GET  /api/v1/correspondence/{id}/read-status` — gated on
 *   `CORRESPONDENCE_READ_STATUS_VIEW`; callers should permission-gate the UI.
 */
@Injectable({ providedIn: 'root' })
export class CorrespondenceReadTrackingApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  acknowledge(
    correspondenceId: string,
    comment?: string | null
  ): Observable<CorrespondenceReadReceiptDto> {
    const body: CorrespondenceAckRequestDto = { comment: comment ?? null };
    return this.http.post<CorrespondenceReadReceiptDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/ack`,
      body
    );
  }

  readStatus(correspondenceId: string): Observable<CorrespondenceReadStatusSummaryDto> {
    return this.http.get<CorrespondenceReadStatusSummaryDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/read-status`
    );
  }

  private correspondenceItemUrl(id: string): string {
    return apiPathWithId(this.base, AppConstants.API.CORRESPONDENCE, id);
  }
}
