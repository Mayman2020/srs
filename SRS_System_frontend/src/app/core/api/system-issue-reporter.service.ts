import { Inject, Injectable } from '@angular/core';
import { API_BASE_URL } from './api-url';
import { AppConstants, apiPath } from '../constants/app-constants';

/** Uses fetch to avoid interceptor loops when reporting HTTP failures. */
@Injectable({ providedIn: 'root' })
export class SystemIssueReporterService {
  constructor(@Inject(API_BASE_URL) private apiBase: string) {}

  reportClientError(opts: {
    severity: 'ERROR' | 'WARN' | 'INFO';
    message: string;
    detail?: string | null;
    httpStatus?: number | null;
  }): void {
    const url = apiPath(this.apiBase, AppConstants.API.SYSTEM_ISSUES_REPORT);
    const pageUrl = typeof location !== 'undefined' ? location.href : null;
    const body = JSON.stringify({
      severity: opts.severity,
      message: opts.message.slice(0, 2000),
      detail: opts.detail ? opts.detail.slice(0, 12000) : null,
      pageUrl: pageUrl ? pageUrl.slice(0, 2000) : null,
      httpStatus: opts.httpStatus ?? null
    });
    fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body
    }).catch(() => {
      /* ignore */
    });
  }
}
