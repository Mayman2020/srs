import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LookupCatalogDto, LookupRowAdminDto } from './api-types';

export interface LookupUpsertBody {
  code: string;
  nameAr: string;
  nameEn: string;
  description?: string | null;
  sortOrder?: number | null;
  active?: boolean | null;
  parentId?: number | null;
  terminal?: boolean | null;
  slaDays?: number | null;
  restrictsExport?: boolean | null;
  requiresClearance?: boolean | null;
  uiVariant?: string | null;
}

@Injectable({ providedIn: 'root' })
export class LookupTableAdminApiService {
  private readonly root: string;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) base: string
  ) {
    this.root = `${base}/admin/lookup-tables`;
  }

  catalog(): Observable<LookupCatalogDto[]> {
    return this.http.get<LookupCatalogDto[]>(`${this.root}/catalog`);
  }

  listRows(lookupCode: string): Observable<LookupRowAdminDto[]> {
    return this.http.get<LookupRowAdminDto[]>(`${this.root}/${encodeURIComponent(lookupCode)}/rows`);
  }

  create(lookupCode: string, body: LookupUpsertBody): Observable<LookupRowAdminDto> {
    return this.http.post<LookupRowAdminDto>(
      `${this.root}/${encodeURIComponent(lookupCode)}/rows`,
      body
    );
  }

  update(lookupCode: string, id: number, body: LookupUpsertBody): Observable<LookupRowAdminDto> {
    return this.http.put<LookupRowAdminDto>(
      `${this.root}/${encodeURIComponent(lookupCode)}/rows/${id}`,
      body
    );
  }

  delete(lookupCode: string, id: number): Observable<void> {
    return this.http.delete<void>(`${this.root}/${encodeURIComponent(lookupCode)}/rows/${id}`);
  }
}
