import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AttachmentDownloadIntentDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/**
 * Slice 5 — signed-download intent + token URL builder.
 *
 * The flow is two requests:
 *   1. `POST /attachments/{id}/download-intent` returns `{ token, expiresAt }`.
 *   2. `GET /attachments/download/{token}` streams the decrypted bytes; the token is single-use
 *      and short-lived (60s default), enforced server-side by `attachment_download_token`.
 *
 * Consumers should still send the JWT `Authorization` header on the GET request — the token
 * binds to the issuing user, not just possession.
 */
@Injectable({ providedIn: 'root' })
export class AttachmentDownloadApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  requestIntent(attachmentId: number): Observable<AttachmentDownloadIntentDto> {
    return this.http.post<AttachmentDownloadIntentDto>(
      `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}${AppConstants.API.ATTACHMENT_DOWNLOAD_INTENT_SUFFIX}`,
      {}
    );
  }

  /** Resolve the single-use token URL. Pair with the standard JWT Bearer header. */
  tokenDownloadUrl(token: string): string {
    return `${apiPath(this.base, AppConstants.API.ATTACHMENT_TOKEN_DOWNLOAD)}/${encodeURIComponent(token)}`;
  }
}
