import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { DepartmentFlatDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class DepartmentApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<DepartmentFlatDto[]> {
    return this.http.get<DepartmentFlatDto[]>(`${this.base}/departments`);
  }
}
