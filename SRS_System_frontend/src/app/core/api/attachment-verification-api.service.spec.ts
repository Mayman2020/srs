import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-url';
import { AttachmentVerificationApiService } from './attachment-verification-api.service';

describe('AttachmentVerificationApiService', () => {
  let svc: AttachmentVerificationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AttachmentVerificationApiService,
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });
    svc = TestBed.inject(AttachmentVerificationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('issues a verification token via POST', () => {
    let captured: { token: string } | undefined;
    svc.issue(42, { ttlDays: 7 }).subscribe((res) => (captured = res));
    const req = http.expectOne('/api/v1/attachments/42/verification-tokens');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ ttlDays: 7 });
    req.flush({
      id: 't1',
      attachmentVersionId: 99,
      token: 'raw-token',
      issuedAt: '2026-01-01T00:00:00Z',
      expiresAt: '2026-01-08T00:00:00Z'
    });
    expect(captured?.token).toBe('raw-token');
  });

  it('uses GET on /public/verify/{token} for public verification', () => {
    svc.publicVerify('abc def').subscribe();
    const req = http.expectOne('/api/v1/public/verify/abc%20def');
    expect(req.request.method).toBe('GET');
    req.flush({
      attachmentVersionId: 1,
      plaintextSha256: 'abc',
      encryptionAlgo: 'AES_256_GCM',
      issuedAt: '2026-01-01T00:00:00Z',
      correspondenceReferenceNumber: 'REF-1',
      organizationLabel: 'Org',
      signatures: []
    });
  });
});
