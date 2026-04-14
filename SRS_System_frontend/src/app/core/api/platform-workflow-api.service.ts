import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPathWithId } from '../constants/app-constants';

/** Monolith workflow helpers (Camunda tasks tied to correspondence). */
export interface CorrespondenceWorkflowDelegateRequest {
  delegateeUserId: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformWorkflowApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  /** Delegates the current user’s active task on this correspondence. */
  delegateCorrespondence(
    correspondenceId: string,
    body: CorrespondenceWorkflowDelegateRequest
  ): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.CORRESPONDENCE, correspondenceId)}/workflow-delegate`,
      body
    );
  }
}
