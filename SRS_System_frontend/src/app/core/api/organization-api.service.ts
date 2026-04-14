import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { OrganizationFlatDto } from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class OrganizationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<OrganizationFlatDto[]> {
    return this.http.get<OrganizationFlatDto[]>(apiPath(this.base, AppConstants.API.ORGANIZATIONS));
  }
}
