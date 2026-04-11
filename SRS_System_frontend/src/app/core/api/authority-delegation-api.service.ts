import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AuthorityDelegationDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class AuthorityDelegationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<AuthorityDelegationDto[]> {
    return this.http.get<AuthorityDelegationDto[]>(`${this.base}/authority-delegations`);
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
    return this.http.post<AuthorityDelegationDto>(`${this.base}/authority-delegations`, body);
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/authority-delegations/${id}`);
  }
}
