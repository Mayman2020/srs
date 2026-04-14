import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { withSilentSuccess } from '../interceptors/http-notification-context';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';
import {
  PermissionAdminDto,
  ServiceWorkflowRouteDto,
  SystemIssueDto,
  UiScreenDto
} from './api-types';

@Injectable({ providedIn: 'root' })
export class AdminConsoleApiService {
  private readonly admin: string;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) base: string
  ) {
    this.admin = apiPath(base, AppConstants.API.ADMIN);
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
    uiScreenId?: number | null;
  }): Observable<PermissionAdminDto> {
    return this.http.post<PermissionAdminDto>(`${this.admin}/permissions`, body, {
      context: withSilentSuccess()
    });
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
      uiScreenId?: number | null;
    }
  ): Observable<PermissionAdminDto> {
    return this.http.put<PermissionAdminDto>(`${this.admin}/permissions/${id}`, body, {
      context: withSilentSuccess()
    });
  }

  deletePermission(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/permissions/${id}`, {
      context: withSilentSuccess()
    });
  }

  getRolePermissionIds(roleId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.admin}/roles/${roleId}/permissions`);
  }

  saveRolePermissionIds(roleId: number, permissionIds: number[]): Observable<void> {
    return this.http.put<void>(
      `${this.admin}/roles/${roleId}/permissions`,
      { permissionIds },
      { context: withSilentSuccess() }
    );
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
    iconKey?: string | null;
    showInShellNav?: boolean | null;
    requiredPermissionId?: number | null;
  }): Observable<UiScreenDto> {
    return this.http.post<UiScreenDto>(`${this.admin}/ui-screens`, body, {
      context: withSilentSuccess()
    });
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
      iconKey?: string | null;
      showInShellNav?: boolean | null;
      requiredPermissionId?: number | null;
    }
  ): Observable<UiScreenDto> {
    return this.http.put<UiScreenDto>(`${this.admin}/ui-screens/${id}`, body, {
      context: withSilentSuccess()
    });
  }

  deleteUiScreen(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/ui-screens/${id}`, {
      context: withSilentSuccess()
    });
  }

  listSystemIssues(): Observable<SystemIssueDto[]> {
    return this.http.get<SystemIssueDto[]>(`${this.admin}/system-issues`);
  }

  resolveSystemIssue(id: number, resolutionNote?: string | null): Observable<SystemIssueDto> {
    return this.http.patch<SystemIssueDto>(
      `${this.admin}/system-issues/${id}/resolve`,
      {
        resolutionNote: resolutionNote ?? null
      },
      { context: withSilentSuccess() }
    );
  }

  listWorkflowRoutes(): Observable<ServiceWorkflowRouteDto[]> {
    return this.http.get<ServiceWorkflowRouteDto[]>(`${this.admin}/workflow-routes`);
  }

  createWorkflowRoute(body: {
    correspondenceTypeId: number;
    processDefinitionKey: string;
    nameAr: string;
    nameEn: string;
    defaultRoute: boolean;
    sortOrder: number;
    active: boolean;
  }): Observable<ServiceWorkflowRouteDto> {
    return this.http.post<ServiceWorkflowRouteDto>(`${this.admin}/workflow-routes`, body, {
      context: withSilentSuccess()
    });
  }

  updateWorkflowRoute(
    id: number,
    body: {
      correspondenceTypeId: number;
      processDefinitionKey: string;
      nameAr: string;
      nameEn: string;
      defaultRoute: boolean;
      sortOrder: number;
      active: boolean;
    }
  ): Observable<ServiceWorkflowRouteDto> {
    return this.http.put<ServiceWorkflowRouteDto>(`${this.admin}/workflow-routes/${id}`, body, {
      context: withSilentSuccess()
    });
  }

  deleteWorkflowRoute(id: number): Observable<void> {
    return this.http.delete<void>(`${this.admin}/workflow-routes/${id}`, {
      context: withSilentSuccess()
    });
  }
}
