import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

export interface AssistantActionDto {
  id: string;
  label: string;
  route?: string | null;
  prompt?: string | null;
}

export interface AssistantAnswerResponseDto {
  text: string;
  actions: AssistantActionDto[];
  llmUsed: boolean;
}

@Injectable({ providedIn: 'root' })
export class AssistantApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  answer(query: string): Observable<AssistantAnswerResponseDto> {
    return this.http.post<AssistantAnswerResponseDto>(
      apiPath(this.base, AppConstants.API.ASSISTANT_ANSWER),
      { query }
    );
  }
}
