import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  DashboardBucketDto,
  DepartmentSlaRowDto,
  ReportMonthlyPointDto,
  WorkflowSlaPointDto
} from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class ReportsApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  statusDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.reportsUrl}/status-distribution`);
  }

  priorityDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.reportsUrl}/priority-distribution`);
  }

  orgLevelDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.reportsUrl}/org-level-distribution`);
  }

  confidentialityDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.reportsUrl}/confidentiality-distribution`);
  }

  workflowSlaTrend(from?: string, to?: string): Observable<WorkflowSlaPointDto[]> {
    let p = new HttpParams();
    if (from) p = p.set('from', from);
    if (to) p = p.set('to', to);
    return this.http.get<WorkflowSlaPointDto[]>(`${this.reportsUrl}/workflow-sla-trend`, {
      params: p
    });
  }

  monthlyTrend(from?: string, to?: string): Observable<ReportMonthlyPointDto[]> {
    let p = new HttpParams();
    if (from) p = p.set('from', from);
    if (to) p = p.set('to', to);
    return this.http.get<ReportMonthlyPointDto[]>(`${this.reportsUrl}/monthly-trend`, {
      params: p
    });
  }

  departmentSlaHeatmap(): Observable<DepartmentSlaRowDto[]> {
    return this.http.get<DepartmentSlaRowDto[]>(`${this.reportsUrl}/department-sla-heatmap`);
  }

  exportExcelBlob(): Observable<Blob> {
    return this.http.get(`${this.reportsUrl}/export/excel`, { responseType: 'blob' });
  }

  private get reportsUrl(): string {
    return apiPath(this.base, AppConstants.API.REPORTS);
  }
}
