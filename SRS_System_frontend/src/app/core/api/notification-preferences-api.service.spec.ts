import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-url';
import {
  NotificationPreferenceRowDto,
  NotificationPreferenceUpsertDto,
  NotificationPreferencesApiService
} from './notification-preferences-api.service';

describe('NotificationPreferencesApiService', () => {
  let svc: NotificationPreferencesApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        NotificationPreferencesApiService,
        { provide: API_BASE_URL, useValue: '/api/v1' }
      ]
    });
    svc = TestBed.inject(NotificationPreferencesApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists current preferences', () => {
    let result: NotificationPreferenceRowDto[] | undefined;
    svc.list().subscribe((rows) => (result = rows));
    const req = http.expectOne('/api/v1/me/notification-preferences');
    expect(req.request.method).toBe('GET');
    const body: NotificationPreferenceRowDto[] = [
      { id: 'a', eventTypeCode: 'WORKFLOW_ASSIGNED', channelCode: 'IN_APP', enabled: true }
    ];
    req.flush(body);
    expect(result).toEqual(body);
  });

  it('replaces preferences via PUT with the full list', () => {
    const body: NotificationPreferenceUpsertDto[] = [
      { eventTypeCode: 'WORKFLOW_ASSIGNED', channelCode: 'IN_APP', enabled: true },
      { eventTypeCode: 'WORKFLOW_ASSIGNED', channelCode: 'EMAIL', enabled: false }
    ];
    svc.replace(body).subscribe();
    const req = http.expectOne('/api/v1/me/notification-preferences');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(body);
    req.flush(null);
  });
});
