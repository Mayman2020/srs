import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AuthorityDelegationDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class AuthorityDelegationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<AuthorityDelegationDto[]> {
    return this.http.get<AuthorityDelegationDto[]>(
      apiPath(this.base, AppConstants.API.AUTHORITY_DELEGATIONS)
    );
  }

  create(body: {
    delegateUserId: string;
    validFrom: string;
    validTo: string;
    allowedCorrespondenceTypeCodes?: string | null;
    allowedConfidentialityCodes?: string | null;
    canSignOnBehalf?: boolean;
    notes?: string | null;
  }): Observable<AuthorityDelegationDto> {
    return this.http.post<AuthorityDelegationDto>(
      apiPath(this.base, AppConstants.API.AUTHORITY_DELEGATIONS),
      body
    );
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.AUTHORITY_DELEGATIONS, id)
    );
  }
}
