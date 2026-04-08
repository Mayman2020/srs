import { InjectionToken } from '@angular/core';

/**
 * Relative API root during `ng serve` (see `proxy.conf.json`).
 * Browser calls `/api/v1/...`; the dev proxy forwards `/api` to the Spring Boot server
 * (`SERVER_PORT`, default 8080 in `application.yml`) → effective base `http://localhost:8080/api/v1`.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1'
});
