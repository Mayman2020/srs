import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LetterTemplateDto } from './api-types';

@Injectable({ providedIn: 'root' })
export class LetterTemplateApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<LetterTemplateDto[]> {
    return this.http.get<LetterTemplateDto[]>(`${this.base}/letter-templates`);
  }
}
