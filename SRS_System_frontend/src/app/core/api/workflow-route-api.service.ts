import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { ServiceWorkflowRouteDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class WorkflowRouteApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  listForCorrespondenceType(correspondenceTypeCode: string): Observable<ServiceWorkflowRouteDto[]> {
    const params = new HttpParams().set('correspondenceTypeCode', correspondenceTypeCode);
    return this.http.get<ServiceWorkflowRouteDto[]>(`${this.base}/workflow-routes`, { params });
  }
}
