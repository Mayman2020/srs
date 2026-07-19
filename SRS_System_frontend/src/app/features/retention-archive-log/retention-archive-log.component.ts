import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  ArchiveTransitionLogDto,
  ArchiveTransitionLogPage,
  RetentionAdminApiService
} from '../../core/api/retention-admin-api.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';

/**
 * Slice 6 — read-only archive transition log. Pages the audit trail of retention lifecycle
 * actions (HARD_DELETE, ANONYMIZE, etc.) executed by the hourly job. Detail JSON is rendered as
 * pre-formatted text behind an `<pre>` so operators can copy/paste evidence into incident notes.
 */
@Component({
  selector: 'app-retention-archive-log',
  standalone: true,
  imports: [CommonModule, TranslatePipe, SrsDatePipe],
  templateUrl: './retention-archive-log.component.html',
  styleUrl: './retention-archive-log.component.scss'
})
export class RetentionArchiveLogComponent implements OnInit {
  private readonly api = inject(RetentionAdminApiService);

  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly page = signal<ArchiveTransitionLogPage | null>(null);
  readonly rows = signal<ArchiveTransitionLogDto[]>([]);
  readonly selected = signal<ArchiveTransitionLogDto | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.pageArchiveLog(this.pageIndex(), this.pageSize()).subscribe({
      next: (p) => {
        this.page.set(p);
        this.rows.set(p.content ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.page.set(null);
        this.rows.set([]);
        this.error.set('retention.log.loadFailed');
        this.loading.set(false);
      }
    });
  }

  prevPage(): void {
    if (this.pageIndex() > 0) {
      this.pageIndex.update((v) => v - 1);
      this.refresh();
    }
  }

  nextPage(): void {
    const p = this.page();
    if (p && !p.last) {
      this.pageIndex.update((v) => v + 1);
      this.refresh();
    }
  }

  select(row: ArchiveTransitionLogDto): void {
    this.selected.set(row);
  }

  closeDetails(): void {
    this.selected.set(null);
  }

  prettyJson(detailJson: string | null): string {
    if (!detailJson) return '';
    try {
      return JSON.stringify(JSON.parse(detailJson), null, 2);
    } catch {
      return detailJson;
    }
  }
}
