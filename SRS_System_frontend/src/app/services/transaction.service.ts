import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CorrespondenceApiService, CorrespondenceListParams } from '../core/api/correspondence-api.service';
import {
  CorrespondenceCommentDetailDto,
  CorrespondenceCreateRequest,
  CorrespondenceCreatedResponse,
  CorrespondenceDetailResponse,
  CorrespondenceListItemDto,
  CorrespondenceTimelineEntryDto,
  WorkflowHistoryEntryDto
} from '../core/api/api-types';
import { LookupLabelsService } from '../core/lookup/lookup-labels.service';
import { Transaction, TimelineStep } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  constructor(
    private correspondenceApi: CorrespondenceApiService,
    private lookupLabels: LookupLabelsService
  ) {}

  listPage(params: CorrespondenceListParams = {}): Observable<Transaction[]> {
    return this.correspondenceApi
      .list({ size: params.size ?? 500, page: params.page ?? 0, ...params })
      .pipe(map((p) => (p.content ?? []).map((row) => this.mapListRow(row))));
  }

  /** Full correspondence payload for the details screen. */
  getDetail(id: string): Observable<CorrespondenceDetailResponse> {
    return this.correspondenceApi.getById(id);
  }

  getWorkflowHistory(correspondenceId: string): Observable<WorkflowHistoryEntryDto[]> {
    return this.correspondenceApi.getWorkflowHistory(correspondenceId);
  }

  create(body: CorrespondenceCreateRequest): Observable<CorrespondenceCreatedResponse> {
    return this.correspondenceApi.create(body);
  }

  workflowAction(
    id: string,
    opts?: { action?: 'APPROVE' | 'REJECT' | 'RETURN'; comment?: string | null }
  ): Observable<void> {
    return this.correspondenceApi.workflowAction(id, {
      action: opts?.action,
      comment: opts?.comment ?? undefined
    });
  }

  addComment(
    id: string,
    bodyText: string,
    parentCommentId?: number | null
  ): Observable<CorrespondenceCommentDetailDto> {
    return this.correspondenceApi.addComment(id, {
      body: bodyText,
      parentCommentId: parentCommentId ?? undefined
    });
  }

  detailTimelineToSteps(entries: CorrespondenceTimelineEntryDto[]): TimelineStep[] {
    return entries.map((e) => {
      const actionLabel =
        this.lookupLabels.label('workflowActionType', e.action) !== '\u2014'
          ? this.lookupLabels.label('workflowActionType', e.action)
          : this.lookupLabels.label('workflowHistoryEventType', e.eventTypeCode);
      return {
        action: actionLabel,
        user:
          e.user?.fullNameAr?.trim() ||
          e.user?.fullNameEn?.trim() ||
          e.user?.username ||
          '\u2014',
        date: new Date(e.timestamp),
        note: e.comment ?? undefined
      };
    });
  }

  historyToTimeline(entries: WorkflowHistoryEntryDto[]): TimelineStep[] {
    return entries.map((e) => {
      const ev = this.lookupLabels.label('workflowHistoryEventType', e.eventTypeCode);
      const act = e.workflowActionTypeCode
        ? this.lookupLabels.label('workflowActionType', e.workflowActionTypeCode)
        : '';
      const action = act ? `${ev} / ${act}` : ev;
      return {
        action,
        user: e.actorDisplayName ?? e.actorUserId ?? '\u2014',
        date: new Date(e.occurredAt),
        note: e.primaryCommentText ?? undefined
      };
    });
  }

  private mapListRow(dto: CorrespondenceListItemDto): Transaction {
    const createdAt = dto.createdAt ? new Date(dto.createdAt) : new Date();
    const typeCode = dto.correspondenceType?.code ?? '';
    const statusCode = dto.correspondenceStatus?.code ?? '';
    const priorityCode = dto.priority?.code ?? '';
    let maxDays = 5;
    if (dto.dueDate) {
      const due = new Date(dto.dueDate).getTime();
      const createdMs = createdAt.getTime();
      const diff = Math.ceil((due - createdMs) / 86_400_000);
      if (diff > 0) {
        maxDays = diff;
      }
    }
    return {
      id: dto.id,
      referenceNumber: dto.referenceNumber,
      type: typeCode,
      typeCode,
      status: statusCode,
      statusCode,
      priorityCode,
      subject: dto.subject,
      description: '',
      createdAt,
      secrecy: '',
      from: dto.ownerDepartment?.nameAr ?? dto.ownerDepartment?.code ?? '',
      to: '',
      created: dto.createdAt?.substring(0, 10) ?? createdAt.toISOString().substring(0, 10),
      maxDays,
      dueDateIso: dto.dueDate,
      timeline: [],
      attachments: []
    };
  }
}
