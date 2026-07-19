import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { withSilentSuccess } from '../interceptors/http-notification-context';
import { API_BASE_URL } from './api-url';
import { SpringPage, UserDetailDto, UserListDto, LookupItemDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface EffectivePermissionDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
}

@Injectable({ providedIn: 'root' })
export class UserDirectoryApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(page = 0, size = 20, query = ''): Observable<SpringPage<UserListDto>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .append('sort', 'createdAt,desc');
    if (query.trim()) {
      params = params.set('q', query.trim());
    }
    return this.http.get<SpringPage<UserListDto>>(apiPath(this.base, AppConstants.API.USERS), {
      params
    });
  }

  getOne(userId: string): Observable<UserDetailDto> {
    return this.http.get<UserDetailDto>(apiPathWithId(this.base, AppConstants.API.USERS, userId));
  }

  effectivePermissions(userId: string): Observable<EffectivePermissionDto[]> {
    return this.http.get<EffectivePermissionDto[]>(
      `${apiPathWithId(this.base, AppConstants.API.USERS, userId)}/effective-permissions`
    );
  }

  /** Role picker for user admin (same ADMIN_USER_MANAGE gate as user CRUD). */
  listRoles(): Observable<LookupItemDto[]> {
    return this.http.get<LookupItemDto[]>(`${apiPath(this.base, AppConstants.API.USERS)}/roles`);
  }

  create(body: {
    username: string;
    password: string;
    fullNameAr: string;
    fullNameEn: string;
    email: string;
    departmentId: number;
    securityClearanceId?: number | null;
  }): Observable<UserDetailDto> {
    return this.http.post<UserDetailDto>(apiPath(this.base, AppConstants.API.USERS), body, {
      context: withSilentSuccess()
    });
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
      securityClearanceId?: number | null;
    }
  ): Observable<UserDetailDto> {
    return this.http.put<UserDetailDto>(
      apiPathWithId(this.base, AppConstants.API.USERS, userId),
      body,
      { context: withSilentSuccess() }
    );
  }

  delete(userId: string): Observable<void> {
    return this.http.delete<void>(apiPathWithId(this.base, AppConstants.API.USERS, userId), {
      context: withSilentSuccess()
    });
  }

  assignRole(userId: string, roleId: number): Observable<void> {
    return this.http.post<void>(
      `${apiPathWithId(this.base, AppConstants.API.USERS, userId)}/roles`,
      { roleId },
      { context: withSilentSuccess() }
    );
  }

  assignRoles(userId: string, roleIds: number[]): Observable<void> {
    return this.http.put<void>(
      `${apiPathWithId(this.base, AppConstants.API.USERS, userId)}/roles`,
      { roleIds },
      { context: withSilentSuccess() }
    );
  }

  toggleActive(userId: string): Observable<UserDetailDto> {
    return this.http.patch<UserDetailDto>(
      `${apiPathWithId(this.base, AppConstants.API.USERS, userId)}/toggle-active`,
      null,
      { context: withSilentSuccess() }
    );
  }
}
