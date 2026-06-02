import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

import { CapabilitiesService } from './capabilities.service';
import { AuthTokenService } from './auth-token.service';
import type { UserCapabilitiesDto } from '../api/api-types';

class AuthTokenStub {
  readonly session$ = new BehaviorSubject<{ rev: number }>({ rev: 0 });
  token: string | null = 'fake-jwt';
  getToken(): string | null {
    return this.token;
  }
  emit(): void {
    this.session$.next({ rev: Math.random() });
  }
}

describe('CapabilitiesService', () => {
  let service: CapabilitiesService;
  let http: HttpTestingController;
  let auth: AuthTokenStub;

  beforeEach(() => {
    auth = new AuthTokenStub();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: auth }
      ]
    });
    service = TestBed.inject(CapabilitiesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  function flushCapabilities(dto: Partial<UserCapabilitiesDto> = {}): void {
    const req = http.expectOne('/api/v1/me/capabilities');
    expect(req.request.method).toBe('GET');
    req.flush({
      roles: dto.roles ?? ['USER'],
      permissions: dto.permissions ?? [],
      screens: dto.screens ?? []
    });
  }

  it('loads capabilities and answers can() with canonical codes only', () => {
    service.load().subscribe();
    flushCapabilities({
      permissions: ['DASHBOARD_VIEW', 'CORRESPONDENCE_VIEW']
    });

    expect(service.can('DASHBOARD_VIEW')).toBe(true);
    expect(service.can('CORRESPONDENCE_VIEW')).toBe(true);
    // Legacy aliases that are NOT returned by the backend must return false; templates that rely
    // on aliases must be migrated to the canonical code.
    expect(service.can('CANCEL_TRANSACTION')).toBe(false);
    expect(service.can('NOT_GRANTED')).toBe(false);
  });

  it('canAny returns true when at least one code is granted', () => {
    service.load().subscribe();
    flushCapabilities({ permissions: ['DASHBOARD_VIEW'] });

    expect(service.canAny(['DASHBOARD_VIEW', 'ADMIN_USER_MANAGE'])).toBe(true);
    expect(service.canAny(['ADMIN_USER_MANAGE', 'ADMIN_ROLE_MANAGE'])).toBe(false);
    expect(service.canAny([])).toBe(false);
  });

  it('canAll requires every code to be granted (and rejects empty list)', () => {
    service.load().subscribe();
    flushCapabilities({ permissions: ['DASHBOARD_VIEW', 'CORRESPONDENCE_VIEW'] });

    expect(service.canAll(['DASHBOARD_VIEW', 'CORRESPONDENCE_VIEW'])).toBe(true);
    expect(service.canAll(['DASHBOARD_VIEW', 'ADMIN_USER_MANAGE'])).toBe(false);
    // Empty list must NOT be a free pass (defensive against silent UI leaks).
    expect(service.canAll([])).toBe(false);
  });

  it('clear() drops the cached snapshot and permission set', () => {
    service.load().subscribe();
    flushCapabilities({ permissions: ['DASHBOARD_VIEW'] });
    expect(service.can('DASHBOARD_VIEW')).toBe(true);

    service.clear();

    expect(service.can('DASHBOARD_VIEW')).toBe(false);
    expect(service.getSnapshot()).toBeNull();
  });

  it('ensureReady() returns immediately when snapshot is present', (done) => {
    service.load().subscribe(() => {
      service.ensureReady().subscribe({
        next: () => {
          done();
        }
      });
    });
    flushCapabilities({ permissions: ['DASHBOARD_VIEW'] });
  });

  it('ensureReady() rejects without a token', (done) => {
    auth.token = null;
    let errored = false;
    service.ensureReady().subscribe({
      next: () => {
        errored = false;
      },
      error: () => {
        errored = true;
        expect(errored).toBe(true);
        done();
      }
    });
  });
});
