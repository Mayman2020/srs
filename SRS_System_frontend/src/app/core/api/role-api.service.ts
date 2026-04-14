import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LookupItemDto } from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class RoleApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<LookupItemDto[]> {
    return this.http.get<LookupItemDto[]>(apiPath(this.base, AppConstants.API.ROLES));
  }
}
