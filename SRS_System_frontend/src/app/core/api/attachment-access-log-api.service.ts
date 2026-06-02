import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppConstants, apiPathWithId } from '../constants/app-constants';
import { API_BASE_URL } from './api-url';
import { AttachmentAccessLogDto } from './api-types';

/**
 * Slice 1 — read-only access log for attachments. Both endpoints are gated on
 * `ATTACHMENT_ACCESS_LOG_VIEW`; callers should permission-gate the UI.
 */
@Injectable({ providedIn: 'root' })
export class AttachmentAccessLogApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  forAttachment(attachmentId: number): Observable<AttachmentAccessLogDto[]> {
    return this.http.get<AttachmentAccessLogDto[]>(
      `${this.attachmentItemUrl(attachmentId)}/access-log`
    );
  }

  forCorrespondence(correspondenceId: string): Observable<AttachmentAccessLogDto[]> {
    return this.http.get<AttachmentAccessLogDto[]>(
      `${this.correspondenceItemUrl(correspondenceId)}/attachment-access-log`
    );
  }

  private attachmentItemUrl(id: number): string {
    return apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, id);
  }

  private correspondenceItemUrl(id: string): string {
    return apiPathWithId(this.base, AppConstants.API.CORRESPONDENCE, id);
  }
}
