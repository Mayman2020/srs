import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { DashboardResponseDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  getDashboard(): Observable<DashboardResponseDto> {
    return this.http.get<DashboardResponseDto>(`${this.base}/dashboard`);
  }
}
