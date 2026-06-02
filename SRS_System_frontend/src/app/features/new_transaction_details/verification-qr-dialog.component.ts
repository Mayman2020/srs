import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  AttachmentVerificationApiService,
  AttachmentVerificationTokenIssuedDto
} from '../../core/api/attachment-verification-api.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';

export interface VerificationQrDialogData {
  attachmentId: number;
  fileLabel: string;
}

/**
 * Slice 6 — verification QR issuance dialog. Posts to `/attachments/{id}/verification-tokens`
 * to mint a one-shot raw token (returned exactly once), then renders a 220px QR code pointing
 * to `${origin}/verify/${token}`. The dialog never persists or re-fetches the token.
 */
@Component({
  selector: 'app-verification-qr-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, TranslatePipe],
  templateUrl: './verification-qr-dialog.component.html',
  styleUrl: './verification-qr-dialog.component.scss'
})
export class VerificationQrDialogComponent implements OnInit {
  private readonly api = inject(AttachmentVerificationApiService);
  private readonly toast = inject(NotificationService);
  readonly dialogRef = inject(MatDialogRef<VerificationQrDialogComponent>);

  readonly loading = signal(true);
  readonly errorKey = signal<string | null>(null);
  readonly issued = signal<AttachmentVerificationTokenIssuedDto | null>(null);
  readonly verifyUrl = signal<string | null>(null);
  readonly qrDataUrl = signal<string | null>(null);
  readonly copied = signal(false);

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: VerificationQrDialogData) {}

  ngOnInit(): void {
    this.api.issue(this.data.attachmentId, {}).subscribe({
      next: async (res) => {
        this.issued.set(res);
        const verifyUrl = `${window.location.origin}/verify/${encodeURIComponent(res.token)}`;
        this.verifyUrl.set(verifyUrl);
        try {
          const QR = await import('qrcode');
          const dataUrl = await QR.toDataURL(verifyUrl, { width: 240, margin: 1 });
          this.qrDataUrl.set(dataUrl);
        } catch {
          this.errorKey.set('verify.qrError');
        }
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('verify.issueFailed');
        this.loading.set(false);
      }
    });
  }

  async copyUrl(): Promise<void> {
    const u = this.verifyUrl();
    if (!u) {
      return;
    }
    try {
      await navigator.clipboard.writeText(u);
      this.copied.set(true);
      this.toast.success('verify.urlCopiedToast');
      setTimeout(() => this.copied.set(false), 1500);
    } catch {
      this.toast.error('verify.urlCopyFailed');
    }
  }

  print(): void {
    const u = this.verifyUrl();
    const data = this.qrDataUrl();
    if (!u || !data) {
      return;
    }
    const w = window.open('', '_blank', 'noopener,width=480,height=600');
    if (!w) {
      this.toast.error('verify.printPopupBlocked');
      return;
    }
    const safeLabel = this.escapeHtml(this.data.fileLabel ?? '');
    const safeUrl = this.escapeHtml(u);
    w.document.write(
      '<!DOCTYPE html><html><head><meta charset="utf-8"><title>QR</title>' +
        '<style>body{font-family:system-ui,sans-serif;text-align:center;padding:2rem}img{margin:1rem 0}code{word-break:break-all}</style>' +
        '</head><body>' +
        '<h3>' + safeLabel + '</h3>' +
        '<img src="' + data + '" width="240" height="240" alt="Verification QR" />' +
        '<p><code>' + safeUrl + '</code></p>' +
        '<script>window.onload=()=>window.print()</script>' +
        '</body></html>'
    );
    w.document.close();
  }

  private escapeHtml(s: string): string {
    return s
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}
