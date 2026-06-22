import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  CreateSlaPolicyRequestDto,
  SlaBreachEventDto,
  SlaEscalationActionTypeDto,
  SlaPolicyDto
} from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

/**
 * Slice 3 — SLA Policy Engine admin API. Reads are gated by `SLA_POLICY_VIEW`; writes by
 * `SLA_POLICY_MANAGE` (backend enforces both via `@PreAuthorize`). The FE keeps both endpoints
 * here so the admin screen + the breach review screen share a single import surface.
 */
@Injectable({ providedIn: 'root' })
export class SlaPolicyApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<SlaPolicyDto[]> {
    return this.http.get<SlaPolicyDto[]>(apiPath(this.base, AppConstants.API.SLA_POLICIES_ADMIN));
  }

  get(id: number): Observable<SlaPolicyDto> {
    return this.http.get<SlaPolicyDto>(
      apiPathWithId(this.base, AppConstants.API.SLA_POLICIES_ADMIN, id)
    );
  }

  create(body: CreateSlaPolicyRequestDto): Observable<SlaPolicyDto> {
    return this.http.post<SlaPolicyDto>(
      apiPath(this.base, AppConstants.API.SLA_POLICIES_ADMIN),
      body
    );
  }

  update(id: number, body: CreateSlaPolicyRequestDto): Observable<SlaPolicyDto> {
    return this.http.put<SlaPolicyDto>(
      apiPathWithId(this.base, AppConstants.API.SLA_POLICIES_ADMIN, id),
      body
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.SLA_POLICIES_ADMIN, id)
    );
  }

  /** Breach ledger (ops view). onlyActive=true by default; pass false for the full feed. */
  listBreaches(onlyActive = true): Observable<SlaBreachEventDto[]> {
    return this.http.get<SlaBreachEventDto[]>(
      `${apiPath(this.base, AppConstants.API.SLA_BREACHES_ADMIN)}?onlyActive=${onlyActive}`
    );
  }

  listEscalationActions(): Observable<SlaEscalationActionTypeDto[]> {
    return this.http.get<SlaEscalationActionTypeDto[]>(
      `${apiPath(this.base, AppConstants.API.SLA_POLICIES_ADMIN)}/escalation-actions`
    );
  }
}
