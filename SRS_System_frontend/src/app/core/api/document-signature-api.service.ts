import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AttachmentVerificationDto, DocumentSignatureDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/** Slice 5 — per-attachment-version digital signatures and the QR/print verifier projection. */
@Injectable({ providedIn: 'root' })
export class DocumentSignatureApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(attachmentId: number): Observable<DocumentSignatureDto[]> {
    return this.http.get<DocumentSignatureDto[]>(
      `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}/signatures`
    );
  }

  create(attachmentId: number): Observable<DocumentSignatureDto> {
    return this.http.post<DocumentSignatureDto>(
      `${apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)}/signatures`,
      {}
    );
  }

  verify(signatureId: string): Observable<DocumentSignatureDto> {
    return this.http.post<DocumentSignatureDto>(
      `${apiPathWithId(this.base, AppConstants.API.SIGNATURES, signatureId)}/verify`,
      {}
    );
  }

  revoke(signatureId: string): Observable<DocumentSignatureDto> {
    return this.http.delete<DocumentSignatureDto>(
      apiPathWithId(this.base, AppConstants.API.SIGNATURES, signatureId)
    );
  }

  verifyAttachmentVersion(versionId: number): Observable<AttachmentVerificationDto> {
    return this.http.get<AttachmentVerificationDto>(
      `${apiPath(this.base, AppConstants.API.VERIFY)}/attachment-versions/${encodeURIComponent(String(versionId))}`
    );
  }
}
