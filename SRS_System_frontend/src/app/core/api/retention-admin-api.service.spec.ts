import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-url';
import {
  RetentionAdminApiService,
  RetentionPolicyAdminDto
} from './retention-admin-api.service';

describe('RetentionAdminApiService', () => {
  let svc: RetentionAdminApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        RetentionAdminApiService,
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });
    svc = TestBed.inject(RetentionAdminApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists retention policies', () => {
    let captured: RetentionPolicyAdminDto[] | undefined;
    svc.listPolicies().subscribe((rows) => (captured = rows));
    const req = http.expectOne('/api/v1/retention/policies');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    expect(captured).toEqual([]);
  });

  it('toggles a policy via PATCH', () => {
    svc.togglePolicy('p1', { enabled: false }).subscribe();
    const req = http.expectOne('/api/v1/retention/policies/p1/enabled');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ enabled: false });
    req.flush({
      id: 'p1',
      code: 'X',
      nameEn: 'X',
      nameAr: 'س',
      appliesTo: 'CORRESPONDENCE',
      retainForDays: 365,
      actionAfter: 'ANONYMIZE',
      enabled: false
    });
  });

  it('places a legal hold', () => {
    svc.placeLegalHold({ correspondenceId: 'c1', reason: 'litigation' }).subscribe();
    const req = http.expectOne('/api/v1/retention/legal-holds');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ correspondenceId: 'c1', reason: 'litigation' });
    req.flush({
      id: 'l1',
      correspondenceId: 'c1',
      reason: 'litigation',
      placedBy: null,
      placedAt: '2026-01-01T00:00:00Z',
      releasedAt: null,
      releasedBy: null,
      releaseReason: null
    });
  });

  it('releases a legal hold with a reason', () => {
    svc.releaseLegalHold('l1', { releaseReason: 'closed' }).subscribe();
    const req = http.expectOne('/api/v1/retention/legal-holds/l1/release');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ releaseReason: 'closed' });
    req.flush(null);
  });

  it('pages the archive log', () => {
    svc.pageArchiveLog(2, 50).subscribe();
    const req = http.expectOne(
      (r) =>
        r.url === '/api/v1/retention/archive-log' &&
        r.params.get('page') === '2' &&
        r.params.get('size') === '50'
    );
    req.flush({
      content: [],
      number: 2,
      size: 50,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true
    });
  });
});
