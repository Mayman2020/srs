import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AttachmentUploadResponseDto } from './api-types';
import { AppConstants, apiPath, apiPathWithId } from '../constants/app-constants';

@Injectable({ providedIn: 'root' })
export class AttachmentApiService {
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private base: string
  ) {}

  upload(file: File, fileCode?: string | null): Observable<AttachmentUploadResponseDto> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    let params = new HttpParams();
    if (fileCode != null && fileCode !== '') {
      params = params.set('fileCode', fileCode);
    }
    return this.http.post<AttachmentUploadResponseDto>(
      `${apiPath(this.base, AppConstants.API.ATTACHMENTS)}/upload`,
      fd,
      {
      params
      }
    );
  }

  delete(attachmentId: number): Observable<void> {
    return this.http.delete<void>(
      apiPathWithId(this.base, AppConstants.API.ATTACHMENTS, attachmentId)
    );
  }
}
