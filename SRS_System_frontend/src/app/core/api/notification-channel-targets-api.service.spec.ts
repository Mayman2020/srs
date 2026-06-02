import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-url';
import {
  NotificationChannelTargetsApiService
} from './notification-channel-targets-api.service';

describe('NotificationChannelTargetsApiService', () => {
  let svc: NotificationChannelTargetsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NotificationChannelTargetsApiService,
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });
    svc = TestBed.inject(NotificationChannelTargetsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a webhook channel target', () => {
    svc
      .create({
        channelCode: 'WEBHOOK',
        targetCode: 'hub',
        targetUrl: 'https://example.com/hook',
        signingSecretRef: 'AC_HUB_SECRET',
        enabled: true,
        description: 'central hub'
      })
      .subscribe();
    const req = http.expectOne('/api/v1/notification-channel-targets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.targetCode).toBe('hub');
    req.flush({
      id: 't1',
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'https://example.com/hook',
      signingSecretRef: 'AC_HUB_SECRET',
      enabled: true,
      description: 'central hub'
    });
  });

  it('partial-updates an existing target', () => {
    svc.update('t1', { enabled: false }).subscribe();
    const req = http.expectOne('/api/v1/notification-channel-targets/t1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ enabled: false });
    req.flush({
      id: 't1',
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'https://example.com/hook',
      signingSecretRef: 'AC_HUB_SECRET',
      enabled: false,
      description: null
    });
  });

  it('deletes a target', () => {
    svc.delete('t1').subscribe();
    const req = http.expectOne('/api/v1/notification-channel-targets/t1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
