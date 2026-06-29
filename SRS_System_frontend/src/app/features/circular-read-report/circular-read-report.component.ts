import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import {
  CircularReadRecipientRowDto,
  CircularReadReportSummaryDto,
  CircularReadStatusDto,
  PlatformCircularApiService
} from '../../core/api/platform-circular-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';
import { UiFormatService } from '../../core/i18n/ui-format.service';

@Component({
  selector: 'app-circular-read-report',
  standalone: true,
  imports: [CommonModule, TranslatePipe, ErpPageShellComponent, SrsDataTableComponent],
  templateUrl: './circular-read-report.component.html',
  styleUrl: './circular-read-report.component.scss'
})
export class CircularReadReportComponent implements OnInit {
  private readonly api = inject(PlatformCircularApiService);
  private readonly i18n = inject(I18nService);
  private readonly toast = inject(NotificationService);
  private readonly format = inject(UiFormatService);

  rows: CircularReadReportSummaryDto[] = [];
  loading = true;
  detailLoading = false;
  selected: CircularReadStatusDto | null = null;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.readReport().subscribe({
      next: (rows) => {
        this.rows = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
        this.toast.error('circularReadReport.loadFailed');
      }
    });
  }

  openDetail(row: CircularReadReportSummaryDto): void {
    this.detailLoading = true;
    this.selected = null;
    this.api.readStatus(row.circularId).subscribe({
      next: (detail) => {
        this.selected = detail;
        this.detailLoading = false;
      },
      error: () => {
        this.detailLoading = false;
        this.toast.error('circularReadReport.detailFailed');
      }
    });
  }

  closeDetail(): void {
    this.selected = null;
  }

  formatPercent(n: number): string {
    return this.format.formatNumber(Math.round(n * 10) / 10, { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }

  recipientName(r: CircularReadRecipientRowDto): string {
    const name = this.i18n.currentLang() === 'en' ? r.fullNameEn : r.fullNameAr;
    return name || r.username || r.userId;
  }
}
