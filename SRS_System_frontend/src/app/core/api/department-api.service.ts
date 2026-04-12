import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { DepartmentFlatDto } from './api-types';

export interface DepartmentUpsertRequest {
  code: string;
  nameAr: string;
  nameEn: string;
  parentId: number | null;
  sortOrder: number;
}

@Injectable({ providedIn: 'root' })
export class DepartmentApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<DepartmentFlatDto[]> {
    return this.http.get<DepartmentFlatDto[]>(`${this.base}/departments`);
  }

  create(request: DepartmentUpsertRequest): Observable<DepartmentFlatDto> {
    return this.http.post<DepartmentFlatDto>(`${this.base}/departments`, request);
  }

  update(id: number, request: DepartmentUpsertRequest): Observable<DepartmentFlatDto> {
    return this.http.put<DepartmentFlatDto>(`${this.base}/departments/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/departments/${id}`);
  }
}
