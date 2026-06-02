import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NotificationPreferencesComponent } from './notification-preferences.component';
import { API_BASE_URL } from '../../core/api/api-url';
import { I18nService } from '../../core/i18n/i18n.service';

class I18nStub {
  currentLang = () => 'en';
  instant = (k: string) => k;
  get currentDirection() {
    return 'ltr' as const;
  }
}

describe('NotificationPreferencesComponent', () => {
  let fixture: ComponentFixture<NotificationPreferencesComponent>;
  let cmp: NotificationPreferencesComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationPreferencesComponent, HttpClientTestingModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: I18nService, useClass: I18nStub }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NotificationPreferencesComponent);
    cmp = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInit(): void {
    fixture.detectChanges();
    const catalogReq = http.expectOne('/api/v1/notification-catalog');
    catalogReq.flush({
      eventTypes: [
        { code: 'WORKFLOW_ASSIGNED', nameEn: 'Assigned', nameAr: 'مُسند' }
      ],
      channels: [
        { code: 'IN_APP', nameEn: 'In-app', nameAr: 'داخل التطبيق' },
        { code: 'EMAIL', nameEn: 'Email', nameAr: 'بريد' }
      ]
    });
    const prefsReq = http.expectOne('/api/v1/me/notification-preferences');
    prefsReq.flush([
      { id: 'p1', eventTypeCode: 'WORKFLOW_ASSIGNED', channelCode: 'IN_APP', enabled: true },
      { id: 'p2', eventTypeCode: 'WORKFLOW_ASSIGNED', channelCode: 'EMAIL', enabled: false }
    ]);
  }

  it('builds an event×channel grid from the server snapshot', () => {
    flushInit();
    const rows = cmp.rows();
    expect(rows.length).toBe(1);
    expect(rows[0].channels['IN_APP'].enabled).toBe(true);
    expect(rows[0].channels['EMAIL'].enabled).toBe(false);
    expect(cmp.dirty()).toBe(false);
  });

  it('flags a cell dirty when toggled away from the baseline', () => {
    flushInit();
    const row = cmp.rows()[0];
    cmp.toggle(row, 'IN_APP');
    expect(row.channels['IN_APP'].enabled).toBe(false);
    expect(row.channels['IN_APP'].dirty).toBe(true);
    expect(cmp.dirty()).toBe(true);
  });

  it('reset restores baseline and clears dirty flag', () => {
    flushInit();
    const row = cmp.rows()[0];
    cmp.toggle(row, 'EMAIL');
    expect(cmp.dirty()).toBe(true);
    cmp.reset();
    expect(cmp.dirty()).toBe(false);
    expect(row.channels['EMAIL'].enabled).toBe(false);
  });

  it('save PUTs the full set and reloads on success', () => {
    flushInit();
    const row = cmp.rows()[0];
    cmp.toggle(row, 'EMAIL');
    cmp.save();
    const put = http.expectOne(
      (r) => r.url === '/api/v1/me/notification-preferences' && r.method === 'PUT'
    );
    expect(Array.isArray(put.request.body)).toBe(true);
    expect((put.request.body as unknown[]).length).toBe(2);
    put.flush(null);
    http.expectOne('/api/v1/notification-catalog').flush({ eventTypes: [], channels: [] });
    http.expectOne('/api/v1/me/notification-preferences').flush([]);
    expect(cmp.saving()).toBe(false);
  });

  it('exposes signal-based snapshot for the template', () => {
    void of(true);
    expect(cmp.loading()).toBe(true);
  });
});
