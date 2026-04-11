import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-url';
import { AttachmentUploadResponseDto } from './api-types';

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
    return this.http.post<AttachmentUploadResponseDto>(`${this.base}/attachments/upload`, fd, {
      params
    });
  }

  downloadUrl(attachmentId: number): string {
    return `${this.base}/attachments/${attachmentId}/download`;
  }

  delete(attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/attachments/${attachmentId}`);
  }
}
