import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { DashboardChartsDto, DashboardSummaryDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  getSummary(): Observable<DashboardSummaryDto> {
    return this.http.get<DashboardSummaryDto>(`${this.base}/dashboard/summary`);
  }

  getCharts(): Observable<DashboardChartsDto> {
    return this.http.get<DashboardChartsDto>(`${this.base}/dashboard/charts`);
  }
}
