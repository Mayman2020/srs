import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  CreateTaskDelegationRequestDto,
  TaskDelegationDto,
  TaskDelegationListDto
} from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/**
 * Slice 2 — Task Delegation. Distinct service from `AuthorityDelegationApiService` so the
 * existing admin-delegation surface keeps working (no backward incompatible changes).
 */
@Injectable({ providedIn: 'root' })
export class TaskDelegationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  /** Caller-scoped: incoming + outgoing + recently-inactive task delegations. */
  listMine(): Observable<TaskDelegationListDto> {
    return this.http.get<TaskDelegationListDto>(
      `${apiPath(this.base, AppConstants.API.TASK_DELEGATIONS)}/mine`
    );
  }

  create(body: CreateTaskDelegationRequestDto): Observable<TaskDelegationDto> {
    return this.http.post<TaskDelegationDto>(
      apiPath(this.base, AppConstants.API.TASK_DELEGATIONS),
      body
    );
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.TASK_DELEGATIONS, id)
    );
  }
}
