import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface AttachmentVerificationTokenIssueRequestDto {
  ttlDays?: number | null;
  permanent?: boolean | null;
}

export interface AttachmentVerificationTokenIssuedDto {
  id: string;
  attachmentVersionId: number;
  token: string;
  issuedAt: string;
  expiresAt: string | null;
}

export interface AttachmentVerificationTokenSummaryDto {
  id: string;
  attachmentVersionId: number;
  issuedBy: string | null;
  issuedAt: string;
  expiresAt: string | null;
  revokedAt: string | null;
  revokedBy: string | null;
  accessCount: number;
  lastAccessedAt: string | null;
}

export interface AttachmentPublicVerificationPayload {
  attachmentVersionId: number;
  plaintextSha256: string;
  encryptionAlgo: string | null;
  issuedAt: string | null;
  correspondenceReferenceNumber: string | null;
  organizationLabel: string | null;
  signatures: Array<{
    signerDisplayName: string | null;
    algorithm: string | null;
    signedAt: string | null;
    status: string | null;
    verificationStatus: string | null;
  }>;
}

/**
 * Slice 6 — admin API for issuing and revoking attachment verification tokens. The raw token is
 * returned exactly once by {@link issue}; the FE must embed it into a QR or printed barcode
 * immediately (the server never returns it again).
 */
@Injectable({ providedIn: 'root' })
export class AttachmentVerificationApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  issue(
    attachmentId: number,
    body: AttachmentVerificationTokenIssueRequestDto = {}
  ): Observable<AttachmentVerificationTokenIssuedDto> {
    const url = `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}/verification-tokens`;
    return this.http.post<AttachmentVerificationTokenIssuedDto>(url, body);
  }

  list(attachmentId: number): Observable<AttachmentVerificationTokenSummaryDto[]> {
    const url = `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}/verification-tokens`;
    return this.http.get<AttachmentVerificationTokenSummaryDto[]>(url);
  }

  revoke(attachmentId: number, tokenId: string): Observable<void> {
    const url = `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}/verification-tokens/${encodeURIComponent(tokenId)}`;
    return this.http.delete<void>(url);
  }

  publicVerify(token: string): Observable<AttachmentPublicVerificationPayload> {
    const url = `${apiPath(this.base, AppConstants.API.PUBLIC_VERIFY)}/${encodeURIComponent(token)}`;
    return this.http.get<AttachmentPublicVerificationPayload>(url);
  }
}
