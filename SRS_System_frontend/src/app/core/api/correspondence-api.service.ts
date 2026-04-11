import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  CorrespondenceAttachmentFormDto,
  CorrespondenceCreateRequest,
  CorrespondenceCreatedResponse,
  CorrespondenceDetailResponse,
  CorrespondenceCommentDetailDto,
  CorrespondenceAttachmentDetailDto,
  CorrespondenceListItemDto,
  CorrespondenceLinkListItemDto,
  CorrespondenceNonarchivedItemDto,
  AttachmentIndexEntryDto,
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

  cancel(id: string, body: { reason?: string | null } = {}): Observable<void> {
    return this.http.post<void>(`${this.base}/correspondence/${id}/cancel`, body);
  }

  addAttachment(
    id: string,
    payload: CorrespondenceAttachmentFormDto
  ): Observable<CorrespondenceAttachmentDetailDto> {
    return this.http.post<CorrespondenceAttachmentDetailDto>(
      `${this.base}/correspondence/${id}/attachments`,
      payload
    );
  }

  saveDraft(id: string, bodyHtml: string): Observable<void> {
    return this.http.post<void>(`${this.base}/correspondence/${id}/draft`, { bodyHtml });
  }

  sendReply(id: string, bodyHtml: string): Observable<void> {
    return this.http.post<void>(`${this.base}/correspondence/${id}/reply`, { bodyHtml });
  }

  listLinks(correspondenceId: string): Observable<CorrespondenceLinkListItemDto[]> {
    return this.http.get<CorrespondenceLinkListItemDto[]>(
      `${this.base}/correspondence/${correspondenceId}/links`
    );
  }

  addLink(
    correspondenceId: string,
    body: { linkedCorrespondenceId: string; linkKind?: string | null; notes?: string | null }
  ): Observable<CorrespondenceLinkListItemDto> {
    return this.http.post<CorrespondenceLinkListItemDto>(
      `${this.base}/correspondence/${correspondenceId}/links`,
      body
    );
  }

  deleteLink(correspondenceId: string, linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/correspondence/${correspondenceId}/links/${linkId}`);
  }

  listNonarchived(correspondenceId: string): Observable<CorrespondenceNonarchivedItemDto[]> {
    return this.http.get<CorrespondenceNonarchivedItemDto[]>(
      `${this.base}/correspondence/${correspondenceId}/nonarchived-items`
    );
  }

  addNonarchived(
    correspondenceId: string,
    body: { itemType: string; descriptionText?: string | null; quantity: number; sortOrder: number }
  ): Observable<CorrespondenceNonarchivedItemDto> {
    return this.http.post<CorrespondenceNonarchivedItemDto>(
      `${this.base}/correspondence/${correspondenceId}/nonarchived-items`,
      body
    );
  }

  updateNonarchived(
    correspondenceId: string,
    itemId: number,
    body: { itemType: string; descriptionText?: string | null; quantity: number; sortOrder: number }
  ): Observable<CorrespondenceNonarchivedItemDto> {
    return this.http.put<CorrespondenceNonarchivedItemDto>(
      `${this.base}/correspondence/${correspondenceId}/nonarchived-items/${itemId}`,
      body
    );
  }

  deleteNonarchived(correspondenceId: string, itemId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/correspondence/${correspondenceId}/nonarchived-items/${itemId}`
    );
  }

  listAttachmentIndexEntries(attachmentId: number): Observable<AttachmentIndexEntryDto[]> {
    return this.http.get<AttachmentIndexEntryDto[]>(
      `${this.base}/attachments/${attachmentId}/index-entries`
    );
  }

  addAttachmentIndexEntry(
    attachmentId: number,
    body: { pageFrom?: number | null; pageTo?: number | null; subjectText?: string | null; sortOrder: number }
  ): Observable<AttachmentIndexEntryDto> {
    return this.http.post<AttachmentIndexEntryDto>(
      `${this.base}/attachments/${attachmentId}/index-entries`,
      body
    );
  }

  updateAttachmentIndexEntry(
    attachmentId: number,
    entryId: number,
    body: { pageFrom?: number | null; pageTo?: number | null; subjectText?: string | null; sortOrder: number }
  ): Observable<AttachmentIndexEntryDto> {
    return this.http.put<AttachmentIndexEntryDto>(
      `${this.base}/attachments/${attachmentId}/index-entries/${entryId}`,
      body
    );
  }

  deleteAttachmentIndexEntry(attachmentId: number, entryId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/attachments/${attachmentId}/index-entries/${entryId}`
    );
  }
}
