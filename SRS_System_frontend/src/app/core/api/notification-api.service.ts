import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from './api-url';

/** Backend inbox endpoint; extend with typed DTO when notification entity is mapped. */
@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.base}/notifications`).pipe(map((r) => r ?? []));
  }
}
