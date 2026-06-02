import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { API_BASE_URL } from '../../core/api/api-url';
import { AppConstants, apiPath } from '../../core/constants/app-constants';
import {
  AttachmentPublicVerificationPayload
} from '../../core/api/attachment-verification-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

type VerifyState = 'loading' | 'ok' | 'not-found' | 'rate-limited' | 'error';

/**
 * Slice 6 — public verification page. Renders only the scrubbed projection returned by
 * `/api/v1/public/verify/{token}` — never any subject/body/confidentiality field, never an
 * internal user id. Designed for unauthenticated external auditors scanning a printed QR.
 */
@Component({
  selector: 'app-public-verify',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './public-verify.component.html',
  styleUrl: './public-verify.component.scss'
})
export class PublicVerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly base = inject(API_BASE_URL);
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  readonly state = signal<VerifyState>('loading');
  readonly payload = signal<AttachmentPublicVerificationPayload | null>(null);
  readonly dir = signal<'ltr' | 'rtl'>('ltr');

  ngOnInit(): void {
    this.dir.set(this.i18n.currentDirection);
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    if (!token || token.length < 16) {
      this.state.set('not-found');
      return;
    }
    const url = `${apiPath(this.base, AppConstants.API.PUBLIC_VERIFY)}/${encodeURIComponent(token)}`;
    this.http.get<AttachmentPublicVerificationPayload>(url).subscribe({
      next: (data) => {
        this.payload.set(data);
        this.state.set('ok');
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404 || err.status === 410) {
          this.state.set('not-found');
        } else if (err.status === 429) {
          this.state.set('rate-limited');
        } else {
          this.state.set('error');
        }
      }
    });
  }

  formatHashShort(hash: string | null | undefined): string {
    if (!hash) return '—';
    const h = String(hash).trim();
    return h.length > 22 ? `${h.slice(0, 10)}…${h.slice(-8)}` : h;
  }
}
