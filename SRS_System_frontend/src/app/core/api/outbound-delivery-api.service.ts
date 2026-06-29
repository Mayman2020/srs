import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

export interface OutboundDeliveryDto {
  id: number;
  correspondenceId: string;
  correspondenceReferenceNumber: string;
  correspondenceSubject: string;
  channelCode: string;
  statusCode: string;
  recipientLabel: string | null;
  proofReference: string | null;
  notes: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  updatedAt: string;
}

export interface UpsertOutboundDeliveryRequestDto {
  correspondenceId: string;
  channelCode: string;
  statusCode: string;
  recipientLabel?: string | null;
  proofReference?: string | null;
  notes?: string | null;
  sentAt?: string | null;
  deliveredAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class OutboundDeliveryApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly base: string
  ) {}

  list(correspondenceId?: string): Observable<OutboundDeliveryDto[]> {
    let params = new HttpParams();
    if (correspondenceId?.trim()) {
      params = params.set('correspondenceId', correspondenceId.trim());
    }
    return this.http.get<OutboundDeliveryDto[]>(
      apiPath(this.base, AppConstants.API.OUTBOUND_DELIVERIES),
      { params }
    );
  }

  create(body: UpsertOutboundDeliveryRequestDto): Observable<OutboundDeliveryDto> {
    return this.http.post<OutboundDeliveryDto>(
      apiPath(this.base, AppConstants.API.OUTBOUND_DELIVERIES),
      body
    );
  }

  update(id: number, body: UpsertOutboundDeliveryRequestDto): Observable<OutboundDeliveryDto> {
    return this.http.put<OutboundDeliveryDto>(
      apiPathWithId(this.base, AppConstants.API.OUTBOUND_DELIVERIES, id),
      body
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.OUTBOUND_DELIVERIES, id)
    );
  }
}
