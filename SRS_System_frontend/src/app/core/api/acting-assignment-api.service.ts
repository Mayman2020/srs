import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  ActingAssignmentDto,
  ActingAssignmentListDto,
  CreateActingAssignmentRequestDto
} from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/** Slice 4 — time-bounded acting manager coverage (`acting_assignment`). */
@Injectable({ providedIn: 'root' })
export class ActingAssignmentApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  listMine(): Observable<ActingAssignmentListDto> {
    return this.http.get<ActingAssignmentListDto>(
      `${apiPath(this.base, AppConstants.API.ACTING_ASSIGNMENTS)}/mine`
    );
  }

  /** Read-only audit feed (requires `ACTING_ASSIGNMENT_VIEW`). */
  listAudit(): Observable<ActingAssignmentDto[]> {
    return this.http.get<ActingAssignmentDto[]>(
      `${apiPath(this.base, AppConstants.API.ACTING_ASSIGNMENTS)}/audit`
    );
  }

  create(body: CreateActingAssignmentRequestDto): Observable<ActingAssignmentDto> {
    return this.http.post<ActingAssignmentDto>(
      apiPath(this.base, AppConstants.API.ACTING_ASSIGNMENTS),
      body
    );
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.ACTING_ASSIGNMENTS, id)
    );
  }
}
