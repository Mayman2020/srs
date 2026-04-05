import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SystemIssueReporterService } from './system-issue-reporter.service';

/** Sends client-visible failures to system_issue (admin triage). Skips auth and report URL. */
export const systemIssueReporterInterceptor: HttpInterceptorFn = (req, next) => {
  const reporter = inject(SystemIssueReporterService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        const url = err.url ?? req.url ?? '';
        if (
          url.includes('/auth/login') ||
          url.includes('/system-issues/report') ||
          err.status === 0
        ) {
          return throwError(() => err);
        }
        const msg =
          (err as HttpErrorResponse & { userMessage?: string }).userMessage ??
          (typeof err.error === 'string' ? err.error : err.message) ??
          'HTTP error';
        reporter.reportClientError({
          severity: err.status >= 500 ? 'ERROR' : 'WARN',
          message: `${req.method} ${req.url} — ${msg}`.slice(0, 2000),
          detail:
            typeof err.error === 'string'
              ? err.error.slice(0, 4000)
              : JSON.stringify(err.error ?? {}).slice(0, 4000),
          httpStatus: err.status
        });
      }
      return throwError(() => err);
    })
  );
};
