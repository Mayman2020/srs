import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { SpringPage, UserListDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class UserDirectoryApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(page = 0, size = 50): Observable<SpringPage<UserListDto>> {
    return this.http.get<SpringPage<UserListDto>>(`${this.base}/users`, {
      params: { page: String(page), size: String(size) }
    });
  }
}
