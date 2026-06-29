import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { OrganizationFlatDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface UpsertOrganizationRequestDto {
  parentId?: number | null;
  code: string;
  nameAr: string;
  nameEn: string;
  external: boolean;
  description?: string | null;
}

@Injectable({ providedIn: 'root' })
export class OrganizationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<OrganizationFlatDto[]> {
    return this.http.get<OrganizationFlatDto[]>(apiPath(this.base, AppConstants.API.ORGANIZATIONS));
  }

  create(body: UpsertOrganizationRequestDto): Observable<OrganizationFlatDto> {
    return this.http.post<OrganizationFlatDto>(apiPath(this.base, AppConstants.API.ORGANIZATIONS), body);
  }

  update(id: number, body: UpsertOrganizationRequestDto): Observable<OrganizationFlatDto> {
    return this.http.put<OrganizationFlatDto>(
      apiPathWithId(this.base, AppConstants.API.ORGANIZATIONS, id),
      body
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiPathWithId(this.base, AppConstants.API.ORGANIZATIONS, id));
  }
}
