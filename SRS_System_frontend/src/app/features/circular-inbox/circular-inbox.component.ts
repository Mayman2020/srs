import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import {
  PlatformCircularApiService,
  PlatformCircularInboxRow,
} from '../../core/api/platform-circular-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

@Component({
  selector: 'app-circular-inbox',
  standalone: true,
  imports: [CommonModule, TranslatePipe, MatSnackBarModule],
  templateUrl: './circular-inbox.component.html',
  styleUrl: './circular-inbox.component.scss',
})
export class CircularInboxComponent implements OnInit {
  rows: PlatformCircularInboxRow[] = [];
  loading = false;
  /** Localized message when inbox cannot be loaded */
  errorText: string | null = null;

  constructor(
    private circularApi: PlatformCircularApiService,
    private auth: AuthTokenService,
    private i18n: I18nService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const userId = this.auth.getUserId();
    if (!userId?.trim()) {
      this.errorText = this.i18n.instant('circularInbox.noUserId');
      this.rows = [];
      return;
    }
    this.loading = true;
    this.errorText = null;
    this.circularApi.inbox(userId.trim()).subscribe({
      next: (list) => {
        this.rows = list ?? [];
        this.loading = false;
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.loading = false;
        this.rows = [];
        this.errorText =
          err.userMessage ?? this.i18n.instant('circularInbox.loadError');
      },
    });
  }

  markRead(row: PlatformCircularInboxRow): void {
    const userId = this.auth.getUserId()?.trim();
    if (!userId) {
      this.snackBar.open(this.i18n.instant('circularInbox.noUserId'), this.i18n.instant('common.close'), {
        duration: 5000,
      });
      return;
    }
    this.circularApi.markRead(row.id, { userId }).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        const msg = err.userMessage ?? this.i18n.instant('circularInbox.loadError');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 6000 });
      },
    });
  }
}
