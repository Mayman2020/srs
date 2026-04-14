import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LookupBundleDto, LookupItemDto } from './api-types';
import { LookupCode } from '../lookup/lookup-code';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class LookupService {
  private cache$?: Observable<LookupBundleDto>;
  private readonly rowCache = new Map<string, Observable<LookupItemDto[]>>();

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  getBundle(): Observable<LookupBundleDto> {
    if (!this.cache$) {
      this.cache$ = this.http
        .get<LookupBundleDto>(apiPath(this.base, AppConstants.API.LOOKUPS))
        .pipe(shareReplay(1));
    }
    return this.cache$;
  }

  /** Shared lookup endpoint: `GET /api/v1/lookups/{lookupCode}`. */
  getByCode(lookupCode: LookupCode | string): Observable<LookupItemDto[]> {
    const code = String(lookupCode).trim();
    if (!this.rowCache.has(code)) {
      this.rowCache.set(
        code,
        this.http
          .get<LookupItemDto[]>(
            `${apiPath(this.base, AppConstants.API.LOOKUPS)}/${encodeURIComponent(code)}`
          )
          .pipe(shareReplay(1))
      );
    }
    return this.rowCache.get(code)!;
  }

  /** Catalog children by parent `lookup_catalog.lookup_code`; kept for admin-style pickers. */
  getCatalog(parentLookupCode: LookupCode | string): Observable<LookupItemDto[]> {
    const code = String(parentLookupCode).trim();
    return this.http.get<LookupItemDto[]>(
      `${apiPath(this.base, AppConstants.API.LOOKUPS)}/catalog/${encodeURIComponent(code)}`
    );
  }

  /** Same as `getByCode(LookupCode.Priority)`. */
  getPriorityLookup(): Observable<LookupItemDto[]> {
    return this.getByCode(LookupCode.Priority);
  }

  /** Clear cache after admin mutates lookups. */
  invalidate(): void {
    this.cache$ = undefined;
    this.rowCache.clear();
  }
}
