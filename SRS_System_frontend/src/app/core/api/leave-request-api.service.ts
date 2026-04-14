import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LeaveRequestDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class LeaveRequestApiService {
  private readonly api: string;
  private readonly adminApi: string;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) base: string
  ) {
    this.api = apiPath(base, AppConstants.API.LEAVE_REQUESTS);
    this.adminApi = apiPath(base, AppConstants.API.ADMIN_LEAVE_REQUESTS);
  }

  create(body: { startDate: string; endDate: string; reason?: string | null }): Observable<LeaveRequestDto> {
    return this.http.post<LeaveRequestDto>(this.api, body);
  }

  listMine(): Observable<LeaveRequestDto[]> {
    return this.http.get<LeaveRequestDto[]>(`${this.api}/mine`);
  }

  listAllAdmin(): Observable<LeaveRequestDto[]> {
    return this.http.get<LeaveRequestDto[]>(this.adminApi);
  }

  decide(
    id: string,
    body: { statusCode: string; decisionNote?: string | null }
  ): Observable<LeaveRequestDto> {
    return this.http.patch<LeaveRequestDto>(
      `${this.adminApi}/${encodeURIComponent(id)}/decision`,
      body
    );
  }
}
