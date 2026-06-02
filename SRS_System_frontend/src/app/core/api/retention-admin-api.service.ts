import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface RetentionPolicyAdminDto {
  id: string;
  code: string;
  nameEn: string;
  nameAr: string;
  appliesTo: string;
  retainForDays: number | null;
  actionAfter: string;
  enabled: boolean;
}

export interface RetentionPolicyToggleRequestDto {
  enabled: boolean;
}

export interface LegalHoldDto {
  id: string;
  correspondenceId: string;
  reason: string;
  placedBy: string | null;
  placedAt: string;
  releasedAt: string | null;
  releasedBy: string | null;
  releaseReason: string | null;
}

export interface LegalHoldPlaceRequestDto {
  correspondenceId: string;
  reason: string;
}

export interface LegalHoldReleaseRequestDto {
  releaseReason: string;
}

export interface ArchiveTransitionLogDto {
  id: string;
  appliedTo: string;
  resourceId: string;
  policyId: string | null;
  action: string;
  executedAt: string;
  detailJson: string | null;
}

export interface ArchiveTransitionLogPage {
  content: ArchiveTransitionLogDto[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/**
 * Slice 6 — retention admin: list policies, toggle enabled state, manage legal holds, page the
 * archive transition log. The destructive operations (delete/hard-cleanup) are *not* exposed
 * through the FE on purpose — the lifecycle job is the only authoritative writer.
 */
@Injectable({ providedIn: 'root' })
export class RetentionAdminApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  listPolicies(): Observable<RetentionPolicyAdminDto[]> {
    return this.http.get<RetentionPolicyAdminDto[]>(
      apiPath(this.base, AppConstants.API.RETENTION_POLICIES_ADMIN)
    );
  }

  togglePolicy(id: string, body: RetentionPolicyToggleRequestDto): Observable<RetentionPolicyAdminDto> {
    return this.http.patch<RetentionPolicyAdminDto>(
      `${apiPathWithId(this.base, AppConstants.API.RETENTION_POLICIES_ADMIN, id)}/enabled`,
      body
    );
  }

  listActiveLegalHolds(): Observable<LegalHoldDto[]> {
    return this.http.get<LegalHoldDto[]>(
      `${apiPath(this.base, AppConstants.API.LEGAL_HOLDS)}/active`
    );
  }

  placeLegalHold(body: LegalHoldPlaceRequestDto): Observable<LegalHoldDto> {
    return this.http.post<LegalHoldDto>(apiPath(this.base, AppConstants.API.LEGAL_HOLDS), body);
  }

  releaseLegalHold(id: string, body: LegalHoldReleaseRequestDto): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.LEGAL_HOLDS, id)}/release`,
      body
    );
  }

  pageArchiveLog(page: number, size: number): Observable<ArchiveTransitionLogPage> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<ArchiveTransitionLogPage>(
      apiPath(this.base, AppConstants.API.ARCHIVE_LOG_ADMIN),
      { params }
    );
  }
}
