import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-url';
import { LookupLabelsService } from '../core/lookup/lookup-labels.service';
import {
  CorrespondenceListDto,
  SpringPage,
  WorkflowHistoryEntryDto
} from '../core/api/api-types';
import { Transaction, TimelineStep } from '../models/transaction.model';

export interface TransactionPayload {
  type: string;
  secrecy: string;
  subject: string;
  description: string;
  from: string;
  to: string[];
  cc: string[];
  maxDays: number;
  letterContent: string;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string,
    private lookupLabels: LookupLabelsService
  ) {}

  listPage(page = 0, size = 500): Observable<Transaction[]> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http
      .get<SpringPage<CorrespondenceListDto>>(`${this.base}/correspondence`, { params })
      .pipe(map((p) => (p.content ?? []).map((row) => this.mapListRow(row))));
  }

  getById(id: string): Observable<Transaction> {
    return this.http
      .get<CorrespondenceListDto>(`${this.base}/correspondence/${id}`)
      .pipe(map((row) => this.mapListRow(row)));
  }

  getWorkflowHistory(correspondenceId: string): Observable<WorkflowHistoryEntryDto[]> {
    return this.http.get<WorkflowHistoryEntryDto[]>(
      `${this.base}/correspondence/${correspondenceId}/workflow-history`
    );
  }

  /**
   * Maps API timeline entries into legacy `TimelineStep` for existing templates.
   */
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

  createTransaction(payload: TransactionPayload): Observable<unknown> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('auth_token') ?? ''}`
    });
    return this.http.post(`${this.base}/correspondence`, payload, { headers });
  }

  private mapListRow(dto: CorrespondenceListDto): Transaction {
    const createdAt = dto.createdAt ? new Date(dto.createdAt) : new Date();
    return {
      id: dto.id,
      referenceNumber: dto.referenceNumber,
      type: dto.typeCode,
      typeCode: dto.typeCode,
      status: dto.statusCode,
      statusCode: dto.statusCode,
      priorityCode: dto.priorityCode,
      subject: dto.subject,
      description: '',
      createdAt,
      secrecy: '',
      from: '',
      to: '',
      created: dto.createdAt?.substring(0, 10) ?? createdAt.toISOString().substring(0, 10),
      maxDays: 5,
      timeline: [],
      attachments: []
    };
  }
}
