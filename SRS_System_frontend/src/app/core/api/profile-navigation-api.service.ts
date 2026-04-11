import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import type { ShellNavItemDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class ProfileNavigationApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  /** Shell sidebar rows from `ui_screen`, filtered by effective permissions (union of active roles). */
  listNav(): Observable<ShellNavItemDto[]> {
    return this.http.get<ShellNavItemDto[]>(`${this.base}/profile/me/navigation`);
  }
}
