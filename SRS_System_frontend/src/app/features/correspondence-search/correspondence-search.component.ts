import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CorrespondenceApiService, CorrespondenceListParams } from '../../core/api/correspondence-api.service';
import { CorrespondenceListItemDto, UserAuditRefDto } from '../../core/api/api-types';
import { LookupService } from '../../core/api/lookup.service';
import { LookupItemDto } from '../../core/api/api-types';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { LookupLabelDto } from '../../core/api/api-types';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

@Component({
  selector: 'app-correspondence-search',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, LookupTranslatePipe, StatusBadgeComponent],
  templateUrl: './correspondence-search.component.html',
  styleUrl: './correspondence-search.component.css'
})
export class CorrespondenceSearchComponent implements OnInit {
  loading = true;
  rows: CorrespondenceListItemDto[] = [];
  totalElements = 0;
  page = 0;
  pageSize = 20;

  status = '';
  type = '';
  priority = '';
  createdFrom = '';
  createdTo = '';

  correspondenceTypes: LookupItemDto[] = [];
  correspondenceStatuses: LookupItemDto[] = [];
  priorities: LookupItemDto[] = [];

  constructor(
    private readonly api: CorrespondenceApiService,
    private readonly lookup: LookupService,
    private readonly lookupLabels: LookupLabelsService,
    private readonly i18n: I18nService,
    public readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  label(l: LookupLabelDto | null | undefined): string {
    if (!l) return '—';
    return this.i18n.currentLang() === 'en' ? l.nameEn || l.code : l.nameAr || l.code;
  }

  auditUserLabel(u: UserAuditRefDto | null | undefined): string {
    if (!u) return '—';
    return this.i18n.currentLang() === 'en'
      ? u.fullNameEn || u.fullNameAr || '—'
      : u.fullNameAr || u.fullNameEn || '—';
  }

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: this.lookup.getBundle(),
      setLoading: (loading) => (this.loading = loading),
      next: (b) => {
        this.lookupLabels.hydrateFromBundle(b);
        this.correspondenceTypes = b.correspondenceTypes ?? [];
        this.correspondenceStatuses = b.correspondenceStatuses ?? [];
        this.priorities = b.priorities ?? [];
        this.runSearch(0);
      },
      error: () => {
        this.rows = [];
      }
    });
  }

  runSearch(page = 0): void {
    this.page = page;
    const p: CorrespondenceListParams = {
      page: this.page,
      size: this.pageSize,
      sort: ['createdAt,desc'],
      status: this.status || undefined,
      type: this.type || undefined,
      priority: this.priority || undefined,
      createdFrom: this.toInstantStart(this.createdFrom),
      createdTo: this.toInstantEnd(this.createdTo)
    };
    subscribePageLoad({
      cdr: this.cdr,
      source: this.api.list(p),
      setLoading: (loading) => (this.loading = loading),
      next: (pg) => {
        this.rows = pg.content ?? [];
        this.totalElements = pg.totalElements ?? 0;
      },
      error: () => {
        this.rows = [];
        this.totalElements = 0;
      }
    });
  }

  private toInstantStart(d: string): string | undefined {
    if (!d?.trim()) return undefined;
    return `${d.trim()}T00:00:00.000Z`;
  }

  private toInstantEnd(d: string): string | undefined {
    if (!d?.trim()) return undefined;
    return `${d.trim()}T23:59:59.999Z`;
  }

  clearFilters(): void {
    this.status = '';
    this.type = '';
    this.priority = '';
    this.createdFrom = '';
    this.createdTo = '';
    this.runSearch(0);
  }

  openRow(row: CorrespondenceListItemDto): void {
    void this.router.navigate(['/transactions', row.id]);
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.pageSize));
  }

  prevPage(): void {
    if (this.page > 0) this.runSearch(this.page - 1);
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages()) this.runSearch(this.page + 1);
  }
}
