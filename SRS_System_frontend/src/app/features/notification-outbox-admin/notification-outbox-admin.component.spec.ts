import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationOutboxAdminComponent } from './notification-outbox-admin.component';
import { API_BASE_URL } from '../../core/api/api-url';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationOutboxAdminDto } from '../../core/api/notification-outbox-api.service';

class DialogStub {
  openConfirm = vi.fn().mockReturnValue(of(true));
}

describe('NotificationOutboxAdminComponent', () => {
  let fixture: ComponentFixture<NotificationOutboxAdminComponent>;
  let cmp: NotificationOutboxAdminComponent;
  let http: HttpTestingController;
  let dialog: DialogStub;

  beforeEach(async () => {
    dialog = new DialogStub();
    await TestBed.configureTestingModule({
      imports: [NotificationOutboxAdminComponent, HttpClientTestingModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: DialogService, useValue: dialog }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NotificationOutboxAdminComponent);
    cmp = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInit(rows: NotificationOutboxAdminDto[] = []): void {
    fixture.detectChanges();
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-outbox' && r.params.get('status') === 'PENDING'
    );
    req.flush({
      content: rows,
      number: 0,
      size: 20,
      totalElements: rows.length,
      totalPages: 1,
      first: true,
      last: true
    });
  }

  it('loads PENDING rows on init', () => {
    flushInit();
    expect(cmp.rows()).toEqual([]);
    expect(cmp.error()).toBeNull();
  });

  it('switching status refetches with the new filter', () => {
    flushInit();
    cmp.setStatus('DEAD');
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-outbox' && r.params.get('status') === 'DEAD'
    );
    req.flush({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    });
    expect(cmp.status()).toBe('DEAD');
  });

  it('requeue posts and refreshes', () => {
    const row: NotificationOutboxAdminDto = {
      id: 'r1',
      idempotencyKey: null,
      eventTypeCode: 'X',
      channelCode: 'EMAIL',
      recipientUserId: null,
      recipientAddress: 'x@y',
      status: 'DEAD',
      attemptCount: 3,
      nextAttemptAt: null,
      lastAttemptedAt: null,
      lastError: 'boom'
    };
    flushInit([row]);
    cmp.requeue(row);
    const post = http.expectOne('/api/v1/notification-outbox/r1/requeue');
    expect(post.request.method).toBe('POST');
    post.flush(null);
    const refresh = http.expectOne((r) => r.url === '/api/v1/notification-outbox');
    refresh.flush({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    });
  });

  it('cancel asks confirmation before deleting', () => {
    const row: NotificationOutboxAdminDto = {
      id: 'r2',
      idempotencyKey: null,
      eventTypeCode: 'X',
      channelCode: 'EMAIL',
      recipientUserId: null,
      recipientAddress: 'x@y',
      status: 'PENDING',
      attemptCount: 0,
      nextAttemptAt: null,
      lastAttemptedAt: null,
      lastError: null
    };
    flushInit([row]);
    cmp.confirmCancel(row);
    expect(dialog.openConfirm).toHaveBeenCalled();
    const post = http.expectOne('/api/v1/notification-outbox/r2/cancel');
    post.flush(null);
    http.expectOne((r) => r.url === '/api/v1/notification-outbox').flush({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    });
  });

  it('canRequeue is true for FAILED/DEAD/CANCELLED only', () => {
    const make = (status: NotificationOutboxAdminDto['status']): NotificationOutboxAdminDto => ({
      id: 'id',
      idempotencyKey: null,
      eventTypeCode: 'X',
      channelCode: 'Y',
      recipientUserId: null,
      recipientAddress: null,
      status,
      attemptCount: 0,
      nextAttemptAt: null,
      lastAttemptedAt: null,
      lastError: null
    });
    expect(cmp.canRequeue(make('FAILED'))).toBe(true);
    expect(cmp.canRequeue(make('DEAD'))).toBe(true);
    expect(cmp.canRequeue(make('CANCELLED'))).toBe(true);
    expect(cmp.canRequeue(make('SENT'))).toBe(false);
    expect(cmp.canRequeue(make('PENDING'))).toBe(false);
  });
});
