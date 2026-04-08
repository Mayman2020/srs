import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { RoleApiService } from '../api/role-api.service';
import { LookupItemDto } from '../api/api-types';

/**
 * Long-lived caches for semi-static reference data to avoid duplicate HTTP on revisits.
 * Invalidate explicitly after admin mutations if a screen stays open.
 */
@Injectable({ providedIn: 'root' })
export class ReferenceDataCacheService {
  private readonly roleApi = inject(RoleApiService);
  private roles$?: Observable<LookupItemDto[]>;

  /** Shared cold stream; replays last value to new subscribers. */
  roles(): Observable<LookupItemDto[]> {
    if (!this.roles$) {
      this.roles$ = this.roleApi.list().pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.roles$;
  }

  invalidateRoles(): void {
    this.roles$ = undefined;
  }
}
