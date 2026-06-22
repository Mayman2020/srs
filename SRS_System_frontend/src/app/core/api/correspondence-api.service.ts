import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  CorrespondenceAttachmentFormDto,
  CorrespondenceCreateRequestDto,
  CorrespondenceCreatedResponseDto,
  CorrespondenceDetailResponseDto,
  CorrespondenceCommentDetailDto,
  CorrespondenceAttachmentDetailDto,
  CorrespondenceListItemDto,
  CorrespondenceLinkListItemDto,
  CorrespondenceNonarchivedItemDto,
  AttachmentIndexEntryDto,
  SpringPage,
  WorkflowHistoryEntryDto
} from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface CorrespondenceListParams {
  page?: number;
  size?: number;
  sort?: string[];
  status?: string;
  type?: string;
  priority?: string;
  createdFrom?: string;
  createdTo?: string;
  /** Server-side case-insensitive search across reference number / subject / external ref. */
  q?: string;
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
    if (p.q && p.q.trim()) params = params.set('q', p.q.trim());
    const sort = p.sort?.length ? p.sort : ['createdAt,desc'];
    sort.forEach((s) => (params = params.append('sort', s)));
    return this.http.get<SpringPage<CorrespondenceListItemDto>>(this.correspondenceUrl, {
      params
    });
  }

  getById(id: string): Observable<CorrespondenceDetailResponseDto> {
    return this.http.get<CorrespondenceDetailResponseDto>(this.correspondenceItemUrl(id));
  }

  create(body: CorrespondenceCreateRequestDto): Observable<CorrespondenceCreatedResponseDto> {
    return this.http.post<CorrespondenceCreatedResponseDto>(this.correspondenceUrl, body);
  }

  workflowAction(
    id: string,
    body: {
      action?: string | null;
      comment?: string | null;
      targetUserId?: string | null;
      targetDepartmentId?: number | null;
    } = {}
  ): Observable<void> {
    return this.http.post<void>(`${this.correspondenceItemUrl(id)}/actions`, body);
  }

  addComment(
    correspondenceId: string,
    body: { body: string; parentCommentId?: number | null }
  ): Observable<CorrespondenceCommentDetailDto> {
    return this.http.post<CorrespondenceCommentDetailDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/comments`,
      body
    );
  }

  getWorkflowHistory(correspondenceId: string): Observable<WorkflowHistoryEntryDto[]> {
    return this.http.get<WorkflowHistoryEntryDto[]>(
      `${this.correspondenceItemUrl(correspondenceId)}/workflow-history`
    );
  }

  cancel(id: string, body: { reason?: string | null } = {}): Observable<void> {
    return this.http.post<void>(`${this.correspondenceItemUrl(id)}/cancel`, body);
  }

  addAttachment(
    id: string,
    payload: CorrespondenceAttachmentFormDto
  ): Observable<CorrespondenceAttachmentDetailDto> {
    return this.http.post<CorrespondenceAttachmentDetailDto>(
      `${this.correspondenceItemUrl(id)}/attachments`,
      payload
    );
  }

  saveDraft(id: string, bodyHtml: string): Observable<void> {
    return this.http.post<void>(`${this.correspondenceItemUrl(id)}/draft`, { bodyHtml });
  }

  sendReply(id: string, bodyHtml: string): Observable<void> {
    return this.http.post<void>(`${this.correspondenceItemUrl(id)}/reply`, { bodyHtml });
  }

  listLinks(correspondenceId: string): Observable<CorrespondenceLinkListItemDto[]> {
    return this.http.get<CorrespondenceLinkListItemDto[]>(
      `${this.correspondenceItemUrl(correspondenceId)}/links`
    );
  }

  addLink(
    correspondenceId: string,
    body: { linkedCorrespondenceId: string; linkKind?: string | null; notes?: string | null }
  ): Observable<CorrespondenceLinkListItemDto> {
    return this.http.post<CorrespondenceLinkListItemDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/links`,
      body
    );
  }

  deleteLink(correspondenceId: string, linkId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.correspondenceItemUrl(correspondenceId)}/links/${encodeURIComponent(String(linkId))}`
    );
  }

  listNonarchived(correspondenceId: string): Observable<CorrespondenceNonarchivedItemDto[]> {
    return this.http.get<CorrespondenceNonarchivedItemDto[]>(
      `${this.correspondenceItemUrl(correspondenceId)}/nonarchived-items`
    );
  }

  addNonarchived(
    correspondenceId: string,
    body: { itemType: string; descriptionText?: string | null; quantity: number; sortOrder: number }
  ): Observable<CorrespondenceNonarchivedItemDto> {
    return this.http.post<CorrespondenceNonarchivedItemDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/nonarchived-items`,
      body
    );
  }

  updateNonarchived(
    correspondenceId: string,
    itemId: number,
    body: { itemType: string; descriptionText?: string | null; quantity: number; sortOrder: number }
  ): Observable<CorrespondenceNonarchivedItemDto> {
    return this.http.put<CorrespondenceNonarchivedItemDto>(
      `${this.correspondenceItemUrl(correspondenceId)}/nonarchived-items/${encodeURIComponent(String(itemId))}`,
      body
    );
  }

  deleteNonarchived(correspondenceId: string, itemId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.correspondenceItemUrl(correspondenceId)}/nonarchived-items/${encodeURIComponent(String(itemId))}`
    );
  }

  listAttachmentIndexEntries(attachmentId: number): Observable<AttachmentIndexEntryDto[]> {
    return this.http.get<AttachmentIndexEntryDto[]>(
      `${this.attachmentItemUrl(attachmentId)}/index-entries`
    );
  }

  addAttachmentIndexEntry(
    attachmentId: number,
    body: { pageFrom?: number | null; pageTo?: number | null; subjectText?: string | null; sortOrder: number }
  ): Observable<AttachmentIndexEntryDto> {
    return this.http.post<AttachmentIndexEntryDto>(
      `${this.attachmentItemUrl(attachmentId)}/index-entries`,
      body
    );
  }

  updateAttachmentIndexEntry(
    attachmentId: number,
    entryId: number,
    body: { pageFrom?: number | null; pageTo?: number | null; subjectText?: string | null; sortOrder: number }
  ): Observable<AttachmentIndexEntryDto> {
    return this.http.put<AttachmentIndexEntryDto>(
      `${this.attachmentItemUrl(attachmentId)}/index-entries/${encodeURIComponent(String(entryId))}`,
      body
    );
  }

  deleteAttachmentIndexEntry(attachmentId: number, entryId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.attachmentItemUrl(attachmentId)}/index-entries/${encodeURIComponent(String(entryId))}`
    );
  }

  private get correspondenceUrl(): string {
    return apiPath(this.base, AppConstants.API.CORRESPONDENCE);
  }

  private correspondenceItemUrl(id: string): string {
    return apiPathWithId(this.base, AppConstants.API.CORRESPONDENCE, id);
  }

  private attachmentItemUrl(id: number): string {
    return apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, id);
  }
}
