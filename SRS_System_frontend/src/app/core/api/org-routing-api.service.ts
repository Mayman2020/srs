import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

/** Single stop in the routing chain (matches backend {@code RoutingStopDto}). */
export interface RoutingStop {
  departmentId: number;
  departmentCode: string;
  departmentNameAr: string;
  departmentNameEn: string;
  levelCode: string;
  roleCode: string;
  reasonKey: string;
}

/** Routing-preview response (matches backend {@code RoutingChainDto}). */
export interface RoutingChain {
  originator: RoutingStop;
  target: RoutingStop;
  stops: RoutingStop[];
  reasonKey: string;
}

/** Q/L/K/S level row for the levels admin grid. */
export interface OrganizationalUnitLevel {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  description?: string | null;
  rankOrder: number;
  active: boolean;
}

/**
 * Reads against the Q/L/K/S organizational endpoints.
 *
 * Used by:
 *  - the Create Correspondence routing-preview widget
 *  - the Org Levels admin grid
 */
@Injectable({ providedIn: 'root' })
export class OrgRoutingApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  listLevels(): Observable<OrganizationalUnitLevel[]> {
    return this.http.get<OrganizationalUnitLevel[]>(
      apiPath(this.base, AppConstants.API.ORG_LEVELS)
    );
  }

  preview(fromDepartmentId: number, toDepartmentId: number): Observable<RoutingChain> {
    const params = new HttpParams()
      .set('from', String(fromDepartmentId))
      .set('to', String(toDepartmentId));
    return this.http.get<RoutingChain>(
      apiPath(this.base, AppConstants.API.ORG_ROUTING_PREVIEW),
      { params }
    );
  }
}
