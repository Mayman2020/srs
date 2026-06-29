import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { AttachmentAccessLogApiService } from '../../core/api/attachment-access-log-api.service';
import { AttachmentAccessLogDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-attachment-access-log-report',
  standalone: true,
  imports: [CommonModule, TranslatePipe, ErpPageShellComponent, SrsDataTableComponent],
  templateUrl: './attachment-access-log-report.component.html',
  styleUrl: './attachment-access-log-report.component.scss'
})
export class AttachmentAccessLogReportComponent implements OnInit {
  private readonly api = inject(AttachmentAccessLogApiService);
  private readonly i18n = inject(I18nService);
  private readonly toast = inject(NotificationService);

  rows: AttachmentAccessLogDto[] = [];
  loading = true;
  pageIndex = 0;
  pageSize = 50;
  totalElements = 0;
  totalPages = 0;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.listGlobal(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        this.rows = page.content ?? [];
        this.totalElements = page.totalElements ?? 0;
        this.totalPages = page.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
        this.toast.error('attachmentAccessLog.loadFailed');
      }
    });
  }

  prevPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.reload();
    }
  }

  nextPage(): void {
    if (this.pageIndex + 1 < this.totalPages) {
      this.pageIndex++;
      this.reload();
    }
  }

  userName(row: AttachmentAccessLogDto): string {
    const name = this.i18n.currentLang() === 'en' ? row.fullNameEn : row.fullNameAr;
    return name || row.username || row.userId || '—';
  }
}
