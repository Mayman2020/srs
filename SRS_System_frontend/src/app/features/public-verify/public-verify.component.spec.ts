import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { PublicVerifyComponent } from './public-verify.component';
import { API_BASE_URL } from '../../core/api/api-url';
import { I18nService } from '../../core/i18n/i18n.service';

class I18nStub {
  currentLang = () => 'en';
  instant = (k: string) => k;
  get currentDirection() {
    return 'ltr' as const;
  }
}

function withToken(token: string) {
  return {
    provide: ActivatedRoute,
    useValue: { snapshot: { paramMap: convertToParamMap({ token }) } }
  };
}

describe('PublicVerifyComponent', () => {
  let http: HttpTestingController;

  async function setup(token: string): Promise<ComponentFixture<PublicVerifyComponent>> {
    await TestBed.configureTestingModule({
      imports: [PublicVerifyComponent, HttpClientTestingModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: I18nService, useClass: I18nStub },
        withToken(token)
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const fx = TestBed.createComponent(PublicVerifyComponent);
    fx.detectChanges();
    return fx;
  }

  afterEach(() => http && http.verify());

  it('renders ok state with scrubbed metadata only', async () => {
    const fx = await setup('a'.repeat(20));
    const cmp = fx.componentInstance;
    const req = http.expectOne(`/api/v1/public/verify/${encodeURIComponent('a'.repeat(20))}`);
    req.flush({
      attachmentVersionId: 1,
      plaintextSha256: 'abcdef123456',
      encryptionAlgo: 'AES_256_GCM',
      issuedAt: '2026-01-01T00:00:00Z',
      correspondenceReferenceNumber: 'REF-1',
      organizationLabel: 'Org',
      signatures: []
    });
    expect(cmp.state()).toBe('ok');
    const payload = cmp.payload();
    expect(payload?.correspondenceReferenceNumber).toBe('REF-1');
    expect(Object.keys(payload as object)).not.toContain('subject');
    expect(Object.keys(payload as object)).not.toContain('body');
  });

  const matchVerify = (r: { url: string }) => r.url.startsWith('/api/v1/public/verify/');

  it('treats 404 as not-found', async () => {
    const fx = await setup('a'.repeat(20));
    const cmp = fx.componentInstance;
    const req = http.expectOne(matchVerify);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(cmp.state()).toBe('not-found');
  });

  it('treats 410 as not-found (revoked/expired)', async () => {
    const fx = await setup('a'.repeat(20));
    const cmp = fx.componentInstance;
    const req = http.expectOne(matchVerify);
    req.flush({}, { status: 410, statusText: 'Gone' });
    expect(cmp.state()).toBe('not-found');
  });

  it('treats 429 as rate-limited', async () => {
    const fx = await setup('a'.repeat(20));
    const cmp = fx.componentInstance;
    const req = http.expectOne(matchVerify);
    req.flush({}, { status: 429, statusText: 'Too Many Requests' });
    expect(cmp.state()).toBe('rate-limited');
  });

  it('rejects suspiciously short tokens client-side', async () => {
    const fx = await setup('abc');
    const cmp = fx.componentInstance;
    expect(cmp.state()).toBe('not-found');
    http.expectNone(matchVerify);
  });
});
