import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  PlatformCircularApiService,
  PlatformCircularInboxRowDto,
} from '../../core/api/platform-circular-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-circular-inbox',
  standalone: true,
  imports: [
    CommonModule,
    TranslatePipe,
    LatinDigitsPipe,
    SrsDatePipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './circular-inbox.component.html',
  styleUrl: './circular-inbox.component.scss',
})
export class CircularInboxComponent implements OnInit {
  rows: PlatformCircularInboxRowDto[] = [];
  loading = false;
  /** Localized message when inbox cannot be loaded */
  errorText: string | null = null;

  constructor(
    private circularApi: PlatformCircularApiService,
    private i18n: I18nService,
    private notification: NotificationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.errorText = null;
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.loading = v),
      source: this.circularApi.inbox(),
      next: (list) => {
        this.rows = list ?? [];
      },
      error: (err: unknown) => {
        this.rows = [];
        const httpErr = err as HttpErrorResponse & { userMessage?: string };
        this.errorText =
          httpErr.userMessage ?? this.i18n.instant('circularInbox.loadError');
      },
    });
  }

  markRead(row: PlatformCircularInboxRowDto): void {
    this.circularApi.markRead(row.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        const msg = err.userMessage ?? this.i18n.instant('circularInbox.loadError');
        this.notification.errorRaw(msg);
      },
    });
  }
}
