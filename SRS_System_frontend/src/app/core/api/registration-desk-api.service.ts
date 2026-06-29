import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

export type RegistrationDeskMode = 'INBOUND' | 'OUTBOUND';

export interface RegistrationDeskIntakeRequestDto {
  deskMode: RegistrationDeskMode;
  correspondenceTypeCode?: string;
  priorityCode: string;
  confidentialityCode: string;
  classificationCode: string;
  subject: string;
  description?: string | null;
  externalReferenceNumber?: string | null;
  senderOrganizationId?: number | null;
  handoffDepartmentIds?: number[];
}

export interface RegistrationDeskIntakeResponseDto {
  id: string;
  referenceNumber: string;
  barcodeValue: string;
  deskMode: RegistrationDeskMode;
}

export interface RegistrationDeskRowDto {
  id: string;
  referenceNumber: string;
  barcodeValue: string;
  subject: string;
  correspondenceTypeCode: string;
  deskMode: RegistrationDeskMode;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class RegistrationDeskApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  intake(body: RegistrationDeskIntakeRequestDto): Observable<RegistrationDeskIntakeResponseDto> {
    return this.http.post<RegistrationDeskIntakeResponseDto>(
      `${apiPath(this.base, AppConstants.API.REGISTRATION_DESK)}/intake`,
      body
    );
  }

  today(deskMode: RegistrationDeskMode): Observable<RegistrationDeskRowDto[]> {
    const params = new HttpParams().set('deskMode', deskMode);
    return this.http.get<RegistrationDeskRowDto[]>(
      `${apiPath(this.base, AppConstants.API.REGISTRATION_DESK)}/today`,
      { params }
    );
  }
}
