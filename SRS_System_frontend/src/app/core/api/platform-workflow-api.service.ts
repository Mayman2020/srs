import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/** Monolith workflow helpers (Camunda tasks tied to correspondence). */
export interface CorrespondenceWorkflowDelegateRequest {
  delegateeUserId: string;
}

export interface WorkflowTaskInboxRowDto {
  taskId: string;
  taskName: string | null;
  taskDefinitionKey: string | null;
  assigneeUserId: string | null;
  processInstanceId: string;
  createdAt: string | null;
  dueDate: string | null;
  correspondenceId: string | null;
  correspondenceReferenceNumber: string | null;
  correspondenceTitle: string | null;
  correspondenceTypeCode: string | null;
  correspondenceStatusCode: string | null;
  priorityCode: string | null;
  currentLevelCode: string | null;
  currentDepartmentId: number | null;
  // Slice 2 — Task Delegation: present when the row was rewired by an active
  // task_delegation row. Both fields are null/false in the common case.
  originalAssigneeUserId?: string | null;
  actingAsDelegate?: boolean;
  taskDelegationId?: string | null;
  /** Slice 4 — first resolved assignee before acting / delegation overlays. */
  workflowDirectAssigneeUserId?: string | null;
  actingForAbsentUserId?: string | null;
  actingAssignmentId?: string | null;
  /** True when the caller holds the task as acting manager (substitute for absent user). */
  actingAsManager?: boolean;
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

  /** Caller-scoped workflow task inbox (assignee + candidate user/group). */
  myInbox(limit = 100): Observable<WorkflowTaskInboxRowDto[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<WorkflowTaskInboxRowDto[]>(
      `${apiPath(this.base, AppConstants.API.WORKFLOW_TASKS)}/inbox`,
      { params }
    );
  }
}
