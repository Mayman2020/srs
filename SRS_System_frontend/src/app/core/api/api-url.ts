import { InjectionToken } from '@angular/core';

/**
 * Relative API root during `ng serve --configuration=development`.
 * Browser calls `/api/v1/...`; `proxy.conf.json` forwards `/api` to `http://localhost:8080`
 * → effective base URL `http://localhost:8080/api/v1`.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1'
});
