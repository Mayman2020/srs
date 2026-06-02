import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { SlaTaskStatusDto } from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

/**
 * Per-task SLA status read used by the workflow inbox countdown chip and the correspondence
 * details SLA panel. Any authenticated user may call this endpoint for tasks they can already
 * see in their inbox (the inbox is itself permission-gated).
 */
@Injectable({ providedIn: 'root' })
export class SlaTaskStatusApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  getStatus(taskId: string): Observable<SlaTaskStatusDto> {
    return this.http.get<SlaTaskStatusDto>(
      `${apiPath(this.base, AppConstants.API.SLA_TASK_STATUS)}/${encodeURIComponent(taskId)}/status`
    );
  }
}
