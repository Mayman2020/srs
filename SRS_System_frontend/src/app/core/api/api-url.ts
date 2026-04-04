import { InjectionToken } from '@angular/core';

/** Proxied to Spring Boot via `proxy.conf.json` during `ng serve`. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1'
});
