import { Injectable } from '@angular/core';
import { Observable, map, mergeMap, of } from 'rxjs';
import { CorrespondenceApiService, CorrespondenceListParams } from '../api/correspondence-api.service';
import {
  CorrespondenceAttachmentDetailDto,
  CorrespondenceAttachmentFormDto,
  CorrespondenceCommentDetailDto,
  CorrespondenceCreateRequestDto,
  CorrespondenceCreatedResponseDto,
  CorrespondenceDetailResponseDto,
  CorrespondenceListItemDto,
  CorrespondenceTimelineEntryDto,
  SpringPage,
  WorkflowHistoryEntryDto
} from '../api/api-types';
import { LookupLabelsService } from '../lookup/lookup-labels.service';
import { LookupCode } from '../lookup/lookup-code';
import { Transaction, TimelineStep } from '../models/transaction.model';

/**
 * Application facade for correspondence: maps API DTOs to UI {@link Transaction} models,
 * resolves timeline labels via {@link LookupLabelsService}, and delegates mutations to
 * {@link CorrespondenceApiService}. Prefer this service in features; keep HTTP types in `core/api`.
 *
 * @deprecated Phase 9 plan: prefer {@link CorrespondenceApiService} directly. This class is kept
 *     during the transition to canonical naming/routes (`/correspondence/*`); existing callers
 *     (dashboard, transactions list, details, search, smart-assistant) should migrate to a thin
 *     view-model mapper. Will be removed in the next release once all callers are converted.
 */
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
      .list({
        sort: ['createdAt,desc'],
        size: params.size ?? 500,
        page: params.page ?? 0,
        ...params
      })
      .pipe(map((p) => (p.content ?? []).map((row) => this.mapListRow(row))));
  }

  /** Spring page for server-driven tables (sorting / paging / filters on `/correspondence`). */
  listSpringPage(params: CorrespondenceListParams = {}): Observable<SpringPage<Transaction>> {
    return this.correspondenceApi.list(params).pipe(
      map((p) => ({
        ...p,
        content: (p.content ?? []).map((row) => this.mapListRow(row))
      }))
    );
  }

  /**
   * Loads up to `maxRows` list rows matching the same filters as reports / admin tables.
   * Uses backend page size (max 100 per request); suitable for KPI derivation on filtered sets.
   */
  fetchMatchingUpTo(
    filters: Omit<CorrespondenceListParams, 'page' | 'size' | 'sort'>,
    maxRows = 2000,
    sort: string[] = ['createdAt,desc']
  ): Observable<Transaction[]> {
    return this.pullMatchingChunk({ ...filters, sort }, 0, [], maxRows);
  }

  private pullMatchingChunk(
    base: CorrespondenceListParams,
    page: number,
    acc: Transaction[],
    maxRows: number
  ): Observable<Transaction[]> {
    return this.listSpringPage({ ...base, page, size: 100 }).pipe(
      mergeMap((sp) => {
        const batch = sp.content ?? [];
        const merged = [...acc, ...batch];
        const capped = merged.slice(0, maxRows);
        const pageSize = sp.size ?? 100;
        const lastPage = batch.length < pageSize || sp.number >= sp.totalPages - 1 || capped.length >= maxRows;
        if (lastPage) {
          return of(capped);
        }
        return this.pullMatchingChunk(base, page + 1, capped, maxRows);
      })
    );
  }

  /** Full correspondence payload for the details screen. */
  getDetail(id: string): Observable<CorrespondenceDetailResponseDto> {
    return this.correspondenceApi.getById(id);
  }

  getWorkflowHistory(correspondenceId: string): Observable<WorkflowHistoryEntryDto[]> {
    return this.correspondenceApi.getWorkflowHistory(correspondenceId);
  }

  create(body: CorrespondenceCreateRequestDto): Observable<CorrespondenceCreatedResponseDto> {
    return this.correspondenceApi.create(body);
  }

  workflowAction(
    id: string,
    opts?: {
      action?: string | null;
      comment?: string | null;
      targetUserId?: string | null;
      targetDepartmentId?: number | null;
    }
  ): Observable<void> {
    return this.correspondenceApi.workflowAction(id, {
      action: opts?.action,
      comment: opts?.comment ?? undefined,
      targetUserId: opts?.targetUserId ?? undefined,
      targetDepartmentId: opts?.targetDepartmentId ?? undefined,
    });
  }

  cancelCorrespondence(id: string, reason?: string | null): Observable<void> {
    return this.correspondenceApi.cancel(id, { reason: reason ?? undefined });
  }

  saveReplyDraft(id: string, bodyHtml: string): Observable<void> {
    return this.correspondenceApi.saveDraft(id, bodyHtml);
  }

  sendCorrespondenceReply(id: string, bodyHtml: string): Observable<void> {
    return this.correspondenceApi.sendReply(id, bodyHtml);
  }

  addCorrespondenceAttachment(
    id: string,
    payload: CorrespondenceAttachmentFormDto
  ): Observable<CorrespondenceAttachmentDetailDto> {
    return this.correspondenceApi.addAttachment(id, payload);
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
        this.lookupLabels.label(LookupCode.WorkflowActionType, e.action) !== '\u2014'
          ? this.lookupLabels.label(LookupCode.WorkflowActionType, e.action)
          : this.lookupLabels.label(LookupCode.WorkflowHistoryEventType, e.eventTypeCode);
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
      const ev = this.lookupLabels.label(LookupCode.WorkflowHistoryEventType, e.eventTypeCode);
      const act = e.workflowActionTypeCode
        ? this.lookupLabels.label(LookupCode.WorkflowActionType, e.workflowActionTypeCode)
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
      statusUiVariant: dto.correspondenceStatus?.uiVariant ?? null,
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
