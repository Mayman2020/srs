import { InjectionToken } from '@angular/core';

type RuntimeWindow = Window & { __SRS_API_URL__?: string };

function resolveApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const runtime = (window as RuntimeWindow).__SRS_API_URL__;
    if (runtime?.trim()) {
      return runtime.trim().replace(/\/$/, '');
    }
  }
  return '/api/v1';
}

/**
 * API root: `runtime-config.js` (`window.__SRS_API_URL__`) in production builds;
 * during `ng serve` defaults to `/api/v1` (see `proxy.conf.json`).
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => resolveApiBaseUrl()
});
