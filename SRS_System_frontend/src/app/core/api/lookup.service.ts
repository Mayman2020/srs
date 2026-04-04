import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LookupBundleDto, LookupItemDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class LookupService {
  private cache$?: Observable<LookupBundleDto>;

  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  getBundle(): Observable<LookupBundleDto> {
    if (!this.cache$) {
      this.cache$ = this.http.get<LookupBundleDto>(`${this.base}/lookups`).pipe(shareReplay(1));
    }
    return this.cache$;
  }

  /** Same as `bundle().priorities` — for callers that only need priority rows. */
  getPriorityLookup(): Observable<LookupItemDto[]> {
    return this.http.get<LookupItemDto[]>(`${this.base}/lookups/priority`);
  }

  /** Clear cache after admin mutates lookups (future). */
  invalidate(): void {
    this.cache$ = undefined;
  }
}
