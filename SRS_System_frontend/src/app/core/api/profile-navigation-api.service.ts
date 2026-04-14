import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import type { ShellNavItemDto } from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class ProfileNavigationApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  /** Shell sidebar rows from `ui_screen`, filtered by effective permissions (union of active roles). */
  listNav(): Observable<ShellNavItemDto[]> {
    return this.http.get<ShellNavItemDto[]>(
      `${apiPath(this.base, AppConstants.API.PROFILE_ME)}/navigation`
    );
  }
}
