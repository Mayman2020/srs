import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { LetterTemplateDto } from './api-types';
import { AppConstants, apiPath } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class LetterTemplateApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  list(): Observable<LetterTemplateDto[]> {
    return this.http.get<LetterTemplateDto[]>(
      apiPath(this.base, AppConstants.API.LETTER_TEMPLATES)
    );
  }
}
