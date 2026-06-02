import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationChannelsAdminComponent } from './notification-channels-admin.component';
import { API_BASE_URL } from '../../core/api/api-url';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

class DialogStub {
  openConfirm = vi.fn().mockReturnValue(of(true));
}

class ToastStub {
  success = vi.fn();
  error = vi.fn();
  errorRaw = vi.fn();
}

describe('NotificationChannelsAdminComponent', () => {
  let fixture: ComponentFixture<NotificationChannelsAdminComponent>;
  let cmp: NotificationChannelsAdminComponent;
  let http: HttpTestingController;
  let toast: ToastStub;

  beforeEach(async () => {
    toast = new ToastStub();
    await TestBed.configureTestingModule({
      imports: [NotificationChannelsAdminComponent, HttpClientTestingModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: DialogService, useClass: DialogStub },
        { provide: NotificationService, useValue: toast }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NotificationChannelsAdminComponent);
    cmp = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInit(): void {
    fixture.detectChanges();
    http.expectOne('/api/v1/notification-channel-targets').flush([]);
  }

  it('blocks save when webhook URL is not https-like', () => {
    flushInit();
    cmp.startCreate();
    cmp.form.patchValue({
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'not a url',
      signingSecretRef: 'AC_HUB_SECRET'
    });
    cmp.submit();
    expect(toast.error).toHaveBeenCalledWith('notificationAdmin.channels.errorUrl');
    http.expectNone((r) => r.url === '/api/v1/notification-channel-targets' && r.method === 'POST');
  });

  it('blocks save when secret reference is missing or lower-case', () => {
    flushInit();
    cmp.startCreate();
    cmp.form.patchValue({
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'https://example.com/hook',
      signingSecretRef: 'lowercase_only'
    });
    cmp.submit();
    expect(toast.error).toHaveBeenCalledWith('notificationAdmin.channels.errorSecretRef');
    http.expectNone((r) => r.url === '/api/v1/notification-channel-targets' && r.method === 'POST');
  });

  it('posts a valid webhook target', () => {
    flushInit();
    cmp.startCreate();
    cmp.form.patchValue({
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'https://example.com/hook',
      signingSecretRef: 'AC_HUB_SECRET',
      enabled: true,
      description: ''
    });
    cmp.submit();
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-channel-targets' && r.method === 'POST'
    );
    expect(req.request.body.signingSecretRef).toBe('AC_HUB_SECRET');
    req.flush({
      id: 't1',
      channelCode: 'WEBHOOK',
      targetCode: 'hub',
      targetUrl: 'https://example.com/hook',
      signingSecretRef: 'AC_HUB_SECRET',
      enabled: true,
      description: null
    });
    http.expectOne('/api/v1/notification-channel-targets').flush([]);
    expect(toast.success).toHaveBeenCalledWith('notificationAdmin.channels.createdToast');
  });

  it('allows EMAIL rows without URL/secret', () => {
    flushInit();
    cmp.startCreate();
    cmp.form.patchValue({
      channelCode: 'EMAIL',
      targetCode: 'smtp-default',
      targetUrl: '',
      signingSecretRef: '',
      enabled: true
    });
    cmp.submit();
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-channel-targets' && r.method === 'POST'
    );
    expect(req.request.body.channelCode).toBe('EMAIL');
    req.flush({
      id: 't2',
      channelCode: 'EMAIL',
      targetCode: 'smtp-default',
      targetUrl: null,
      signingSecretRef: null,
      enabled: true,
      description: null
    });
    http.expectOne('/api/v1/notification-channel-targets').flush([]);
    expect(toast.success).toHaveBeenCalled();
  });
});
