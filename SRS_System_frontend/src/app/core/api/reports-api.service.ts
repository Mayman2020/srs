import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import {
  DashboardBucketDto,
  DepartmentSlaRowDto,
  ReportMonthlyPointDto
} from './api-types';

@Injectable({ providedIn: 'root' })
export class ReportsApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  statusDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.base}/reports/status-distribution`);
  }

  priorityDistribution(): Observable<DashboardBucketDto[]> {
    return this.http.get<DashboardBucketDto[]>(`${this.base}/reports/priority-distribution`);
  }

  monthlyTrend(from?: string, to?: string): Observable<ReportMonthlyPointDto[]> {
    let p = new HttpParams();
    if (from) p = p.set('from', from);
    if (to) p = p.set('to', to);
    return this.http.get<ReportMonthlyPointDto[]>(`${this.base}/reports/monthly-trend`, {
      params: p
    });
  }

  departmentSlaHeatmap(): Observable<DepartmentSlaRowDto[]> {
    return this.http.get<DepartmentSlaRowDto[]>(
      `${this.base}/reports/department-sla-heatmap`
    );
  }
}
