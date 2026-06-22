import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { withSilentSuccess } from '../interceptors/http-notification-context';
import { API_BASE_URL } from './api-url';
import { LetterTemplateAdminDto, LetterTemplateDto } from './api-types';
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

  listAdmin(): Observable<LetterTemplateAdminDto[]> {
    return this.http.get<LetterTemplateAdminDto[]>(
      `${apiPath(this.base, AppConstants.API.LETTER_TEMPLATES)}/admin`
    );
  }

  create(body: {
    code: string;
    nameAr: string;
    nameEn: string;
    bodyHtml: string;
    templateFilePath?: string | null;
    sortOrder: number;
    active: boolean;
  }): Observable<LetterTemplateAdminDto> {
    return this.http.post<LetterTemplateAdminDto>(
      `${apiPath(this.base, AppConstants.API.LETTER_TEMPLATES)}/admin`,
      body,
      { context: withSilentSuccess() }
    );
  }

  update(
    id: number,
    body: {
      nameAr: string;
      nameEn: string;
      bodyHtml: string;
      templateFilePath?: string | null;
      sortOrder: number;
      active: boolean;
    }
  ): Observable<LetterTemplateAdminDto> {
    return this.http.put<LetterTemplateAdminDto>(
      `${apiPath(this.base, AppConstants.API.LETTER_TEMPLATES)}/admin/${encodeURIComponent(String(id))}`,
      body,
      { context: withSilentSuccess() }
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${apiPath(this.base, AppConstants.API.LETTER_TEMPLATES)}/admin/${encodeURIComponent(String(id))}`,
      { context: withSilentSuccess() }
    );
  }
}
