import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { PermissionAdminDto, SystemIssueDto, UiScreenDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class AdminConsoleApiService {
  private readonly admin: string;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) base: string
  ) {
    this.admin = `${base}/admin`;
  }

  listPermissions(): Observable<PermissionAdminDto[]> {
    return this.http.get<PermissionAdminDto[]>(`${this.admin}/permissions`);
  }

  createPermission(body: {
    code: string;
    nameAr: string;
    nameEn: string;
    description?: string | null;
    sortOrder: number;
    active: boolean;
  }): Observable<PermissionAdminDto> {
    return this.http.post<PermissionAdminDto>(`${this.admin}/permissions`, body);
  }

  updatePermission(
    id: number,
    body: {
      code: string;
      nameAr: string;
      nameEn: string;
      description?: string | null;
      sortOrder: number;
      active: boolean;
    }
  ): Observable<PermissionAdminDto> {
    return this.http.put<PermissionAdminDto>(`${this.admin}/permissions/${id}`, body);
  }

  deletePermission(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/permissions/${id}`);
  }

  getRolePermissionIds(roleId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.admin}/roles/${roleId}/permissions`);
  }

  saveRolePermissionIds(roleId: number, permissionIds: number[]): Observable<void> {
    return this.http.put<void>(`${this.admin}/roles/${roleId}/permissions`, { permissionIds });
  }

  listUiScreens(): Observable<UiScreenDto[]> {
    return this.http.get<UiScreenDto[]>(`${this.admin}/ui-screens`);
  }

  createUiScreen(body: {
    code: string;
    routePath: string;
    nameAr: string;
    nameEn: string;
    description?: string | null;
    sortOrder: number;
    active: boolean;
  }): Observable<UiScreenDto> {
    return this.http.post<UiScreenDto>(`${this.admin}/ui-screens`, body);
  }

  updateUiScreen(
    id: number,
    body: {
      code: string;
      routePath: string;
      nameAr: string;
      nameEn: string;
      description?: string | null;
      sortOrder: number;
      active: boolean;
    }
  ): Observable<UiScreenDto> {
    return this.http.put<UiScreenDto>(`${this.admin}/ui-screens/${id}`, body);
  }

  deleteUiScreen(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/ui-screens/${id}`);
  }

  listSystemIssues(): Observable<SystemIssueDto[]> {
    return this.http.get<SystemIssueDto[]>(`${this.admin}/system-issues`);
  }

  resolveSystemIssue(id: number, resolutionNote?: string | null): Observable<SystemIssueDto> {
    return this.http.patch<SystemIssueDto>(`${this.admin}/system-issues/${id}/resolve`, {
      resolutionNote: resolutionNote ?? null
    });
  }
}
