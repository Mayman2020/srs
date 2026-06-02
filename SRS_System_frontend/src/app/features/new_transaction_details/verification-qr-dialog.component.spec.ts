import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { vi } from 'vitest';
import {
  VerificationQrDialogComponent,
  VerificationQrDialogData
} from './verification-qr-dialog.component';
import { API_BASE_URL } from '../../core/api/api-url';
import { NotificationService } from '../../core/services/notification.service';

class DialogRefStub {
  close = vi.fn();
}

class ToastStub {
  success = vi.fn();
  error = vi.fn();
}

describe('VerificationQrDialogComponent', () => {
  let http: HttpTestingController;
  let dialogRef: DialogRefStub;
  let toast: ToastStub;

  async function setup(data: VerificationQrDialogData = { attachmentId: 7, fileLabel: 'file.pdf' }) {
    dialogRef = new DialogRefStub();
    toast = new ToastStub();
    await TestBed.configureTestingModule({
      imports: [VerificationQrDialogComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: API_BASE_URL, useValue: '/api/v1' },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: NotificationService, useValue: toast }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const fx = TestBed.createComponent(VerificationQrDialogComponent);
    fx.detectChanges();
    return fx;
  }

  afterEach(() => http && http.verify());

  it('issues a token on init and exposes the raw token + verify URL', async () => {
    const fx = await setup();
    const cmp = fx.componentInstance;
    const req = http.expectOne('/api/v1/attachments/7/verification-tokens');
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 't1',
      attachmentVersionId: 9,
      token: 'plain-token',
      issuedAt: '2026-01-01T00:00:00Z',
      expiresAt: null
    });
    expect(cmp.issued()?.token).toBe('plain-token');
    expect(cmp.verifyUrl()).toContain('/verify/plain-token');
  });

  it('signals an error when the issuance request fails', async () => {
    const fx = await setup();
    const cmp = fx.componentInstance;
    const req = http.expectOne('/api/v1/attachments/7/verification-tokens');
    req.flush({}, { status: 500, statusText: 'Server Error' });
    expect(cmp.errorKey()).toBe('verify.issueFailed');
    expect(cmp.loading()).toBe(false);
  });
});
