import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-url';
import {
  NotificationOutboxApiService,
  NotificationOutboxPage
} from './notification-outbox-api.service';

describe('NotificationOutboxApiService', () => {
  let svc: NotificationOutboxApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NotificationOutboxApiService,
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });
    svc = TestBed.inject(NotificationOutboxApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pages with status filter', () => {
    let captured: NotificationOutboxPage | undefined;
    svc.page('DEAD', 1, 25).subscribe((p) => (captured = p));
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-outbox' && r.params.get('status') === 'DEAD'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('25');
    req.flush({
      content: [],
      number: 1,
      size: 25,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true
    });
    expect(captured?.size).toBe(25);
  });

  it('omits status when blank', () => {
    svc.page('', 0, 20).subscribe();
    const req = http.expectOne(
      (r) => r.url === '/api/v1/notification-outbox' && !r.params.has('status')
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
  });

  it('posts requeue', () => {
    svc.requeue('abc').subscribe();
    const req = http.expectOne('/api/v1/notification-outbox/abc/requeue');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('posts cancel', () => {
    svc.cancel('abc').subscribe();
    const req = http.expectOne('/api/v1/notification-outbox/abc/cancel');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
