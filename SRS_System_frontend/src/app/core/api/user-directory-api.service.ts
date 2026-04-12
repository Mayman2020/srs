import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { SpringPage, UserDetailDto, UserListDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class UserDirectoryApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(page = 0, size = 50): Observable<SpringPage<UserListDto>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .append('sort', 'createdAt,desc');
    return this.http.get<SpringPage<UserListDto>>(`${this.base}/users`, { params });
  }

  getOne(userId: string): Observable<UserDetailDto> {
    return this.http.get<UserDetailDto>(`${this.base}/users/${userId}`);
  }

  create(body: {
    username: string;
    password: string;
    fullNameAr: string;
    fullNameEn: string;
    email: string;
    departmentId: number;
  }): Observable<UserDetailDto> {
    return this.http.post<UserDetailDto>(`${this.base}/users`, body);
  }

  update(
    userId: string,
    body: {
      fullNameAr: string;
      fullNameEn: string;
      email: string;
      departmentId: number;
      active: boolean;
      password?: string | null;
    }
  ): Observable<UserDetailDto> {
    return this.http.put<UserDetailDto>(`${this.base}/users/${userId}`, body);
  }

  delete(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/users/${userId}`);
  }

  assignRole(userId: string, roleId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/users/${userId}/roles`, { roleId });
  }

  assignRoles(userId: string, roleIds: number[]): Observable<void> {
    return this.http.put<void>(`${this.base}/users/${userId}/roles`, { roleIds });
  }
}
