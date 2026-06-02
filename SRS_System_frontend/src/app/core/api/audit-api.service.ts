import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

/** Audit event row as returned by the backend audit query. */
export interface AuditEventRecord {
  id: string;
  occurredAt: string;
  actorUserId: string;
  actionCode: string;
  resourceType?: string | null;
  resourceId?: string | null;
  detailJson?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
}

/** Filter parameters for the audit events endpoint. */
export interface AuditEventQuery {
  actor?: string;
  action?: string;
  from?: string;
  to?: string;
  limit?: number;
}

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  /** Stream of audit events filtered by actor / action / time. Requires {@code ADMIN_AUDIT_VIEW}. */
  query(filter: AuditEventQuery = {}): Observable<AuditEventRecord[]> {
    let params = new HttpParams();
    if (filter.actor) {
      params = params.set('actor', filter.actor);
    }
    if (filter.action) {
      params = params.set('action', filter.action);
    }
    if (filter.from) {
      params = params.set('from', filter.from);
    }
    if (filter.to) {
      params = params.set('to', filter.to);
    }
    if (filter.limit) {
      params = params.set('limit', String(filter.limit));
    }
    return this.http.get<AuditEventRecord[]>(
      apiPath(this.base, `${AppConstants.API.AUDIT}/events`),
      { params }
    );
  }
}
