import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  CorrespondenceCreateRequest,
  CorrespondenceCreatedResponse,
  CorrespondenceDetailResponse,
  CorrespondenceCommentDetailDto,
  CorrespondenceListItemDto,
  SpringPage,
  WorkflowHistoryEntryDto
} from './api-types';

export interface CorrespondenceListParams {
  page?: number;
  size?: number;
  sort?: string[];
  status?: string;
  type?: string;
  priority?: string;
  createdFrom?: string;
  createdTo?: string;
}

@Injectable({ providedIn: 'root' })
export class CorrespondenceApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(p: CorrespondenceListParams = {}): Observable<SpringPage<CorrespondenceListItemDto>> {
    let params = new HttpParams()
      .set('page', String(p.page ?? 0))
      .set('size', String(p.size ?? 20));
    if (p.status) params = params.set('status', p.status);
    if (p.type) params = params.set('type', p.type);
    if (p.priority) params = params.set('priority', p.priority);
    if (p.createdFrom) params = params.set('createdFrom', p.createdFrom);
    if (p.createdTo) params = params.set('createdTo', p.createdTo);
    const sort = p.sort?.length ? p.sort : ['createdAt,desc'];
    sort.forEach((s) => (params = params.append('sort', s)));
    return this.http.get<SpringPage<CorrespondenceListItemDto>>(`${this.base}/correspondence`, {
      params
    });
  }

  getById(id: string): Observable<CorrespondenceDetailResponse> {
    return this.http.get<CorrespondenceDetailResponse>(`${this.base}/correspondence/${id}`);
  }

  create(body: CorrespondenceCreateRequest): Observable<CorrespondenceCreatedResponse> {
    return this.http.post<CorrespondenceCreatedResponse>(`${this.base}/correspondence`, body);
  }

  workflowAction(
    id: string,
    body: { action?: string | null; comment?: string | null } = {}
  ): Observable<void> {
    return this.http.post<void>(`${this.base}/correspondence/${id}/actions`, body);
  }

  addComment(
    correspondenceId: string,
    body: { body: string; parentCommentId?: number | null }
  ): Observable<CorrespondenceCommentDetailDto> {
    return this.http.post<CorrespondenceCommentDetailDto>(
      `${this.base}/correspondence/${correspondenceId}/comments`,
      body
    );
  }

  getWorkflowHistory(correspondenceId: string): Observable<WorkflowHistoryEntryDto[]> {
    return this.http.get<WorkflowHistoryEntryDto[]>(
      `${this.base}/correspondence/${correspondenceId}/workflow-history`
    );
  }
}
