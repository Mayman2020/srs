import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { LegalHoldsAdminComponent } from './legal-holds-admin.component';
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

describe('LegalHoldsAdminComponent', () => {
  let fixture: ComponentFixture<LegalHoldsAdminComponent>;
  let cmp: LegalHoldsAdminComponent;
  let http: HttpTestingController;
  let dialog: DialogStub;
  let toast: ToastStub;

  beforeEach(async () => {
    dialog = new DialogStub();
    toast = new ToastStub();
    await TestBed.configureTestingModule({
      imports: [LegalHoldsAdminComponent, HttpClientTestingModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: DialogService, useValue: dialog },
        { provide: NotificationService, useValue: toast }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(LegalHoldsAdminComponent);
    cmp = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushInit(): void {
    fixture.detectChanges();
    http
      .expectOne('/api/v1/retention/legal-holds/active')
      .flush([
        {
          id: 'h1',
          correspondenceId: '00000000-0000-4000-8000-000000000000',
          reason: 'litigation',
          placedBy: null,
          placedAt: '2026-01-01T00:00:00Z',
          releasedAt: null,
          releasedBy: null,
          releaseReason: null
        }
      ]);
  }

  it('rejects invalid correspondence id', () => {
    flushInit();
    cmp.form.patchValue({ correspondenceId: 'not-a-uuid', reason: 'because' });
    cmp.place();
    expect(cmp.form.invalid).toBe(true);
    http.expectNone((r) => r.url === '/api/v1/retention/legal-holds' && r.method === 'POST');
  });

  it('places a hold when the form is valid', () => {
    flushInit();
    cmp.form.patchValue({
      correspondenceId: '00000000-0000-4000-8000-000000000000',
      reason: 'pending litigation'
    });
    cmp.place();
    const req = http.expectOne((r) => r.url === '/api/v1/retention/legal-holds' && r.method === 'POST');
    expect(req.request.body.reason).toBe('pending litigation');
    req.flush({
      id: 'new',
      correspondenceId: '00000000-0000-4000-8000-000000000000',
      reason: 'pending litigation',
      placedBy: null,
      placedAt: '2026-01-02T00:00:00Z',
      releasedAt: null,
      releasedBy: null,
      releaseReason: null
    });
    http.expectOne('/api/v1/retention/legal-holds/active').flush([]);
    expect(toast.success).toHaveBeenCalledWith('retention.legalHolds.placedToast');
  });

  it('requires a release reason before confirming a release', () => {
    flushInit();
    const row = cmp.rows()[0];
    cmp.startRelease(row);
    cmp.releaseReason = '';
    cmp.confirmRelease(row);
    expect(dialog.openConfirm).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('retention.legalHolds.releaseReasonRequired');
  });

  it('releases a hold after confirmation', () => {
    flushInit();
    const row = cmp.rows()[0];
    cmp.startRelease(row);
    cmp.releaseReason = 'matter closed';
    cmp.confirmRelease(row);
    expect(dialog.openConfirm).toHaveBeenCalled();
    const req = http.expectOne('/api/v1/retention/legal-holds/h1/release');
    expect(req.request.body).toEqual({ releaseReason: 'matter closed' });
    req.flush(null);
    http.expectOne('/api/v1/retention/legal-holds/active').flush([]);
    expect(toast.success).toHaveBeenCalledWith('retention.legalHolds.releasedToast');
  });
});
