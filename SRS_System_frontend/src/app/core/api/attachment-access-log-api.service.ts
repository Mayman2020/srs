import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';
import { API_BASE_URL } from './api-url';
import { AttachmentAccessLogDto, SpringPage } from './api-types';

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

  listGlobal(page = 0, size = 50): Observable<SpringPage<AttachmentAccessLogDto>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .append('sort', 'occurredAt,desc');
    return this.http.get<SpringPage<AttachmentAccessLogDto>>(
      apiPath(this.base, AppConstants.API.ATTACHMENT_ACCESS_LOG_GLOBAL),
      { params }
    );
  }

  private attachmentItemUrl(id: number): string {
    return apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, id);
  }

  private correspondenceItemUrl(id: string): string {
    return apiPathWithId(this.base, AppConstants.API.CORRESPONDENCE, id);
  }
}
