import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, switchMap, takeUntil } from 'rxjs/operators';

import { Transaction } from '../../core/models/transaction.model';
import { TransactionService } from '../../core/services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { SpringPage } from '../../core/api/api-types';

/**
 * Correspondence list with server-side pagination, sorting, and filtering. Free-text search
 * uses the backend {@code q} parameter (matches reference number / subject / external ref).
 * Status / type / priority pass through to the backend specification, so no client-side
 * filtering is needed.
 */
@Component({
  selector: 'app-transactions-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    LookupTranslatePipe,
    SrsDataTableComponent,
    SrsSortHeaderComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.css'],
  animations: [srsTableRowEnter]
})
export class TransactionsListComponent implements OnInit, OnDestroy {
  /** Rows currently rendered for the active page. */
  pageData: Transaction[] = [];
  /** Compatibility for the existing template — same as {@link pageData}. */
  filtered: Transaction[] = [];

  type = '';

  fNo = '';
  fSubject = '';
  fFrom = '';
  fStatus = '';

  page = 1;
  pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  total = 0;

  tableLoading = true;
  sortColumn = 'createdAt';
  sortDir: SortDirection = 'desc';

  statusFilterCodes: string[] = [];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private service: TransactionService,
    private route: ActivatedRoute,
    public router: Router,
    private lookupLabels: LookupLabelsService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  get listContextLabel(): string {
    if (!this.type) {
      return this.i18n.instant('common.all');
    }
    if (this.type === 'ARCHIVED') {
      return this.lookupLabels.label(LookupCode.CorrespondenceStatus, 'ARCHIVED');
    }
    return this.lookupLabels.label(LookupCode.CorrespondenceType, this.type);
  }

  ngOnInit(): void {
    this.type = this.route.snapshot.paramMap.get('type') || '';

    this.lookupLabels
      .loadTable(LookupCode.CorrespondenceStatus)
      .pipe(takeUntil(this.destroy$), catchError(() => of([])))
      .subscribe((rows) => {
        this.statusFilterCodes = (rows ?? []).map((r) => r.code);
        this.cdr.detectChanges();
      });

    this.reload$
      .pipe(
        debounceTime(200),
        switchMap(() => {
          this.tableLoading = true;
          return this.service
            .listSpringPage({
              page: this.page - 1,
              size: this.pageSize,
              sort: [`${this.toBackendSort(this.sortColumn)},${this.sortDir}`],
              status: this.effectiveStatus(),
              type: this.effectiveType(),
              q: this.composeFreeText()
            })
            .pipe(
              catchError(() =>
                of<SpringPage<Transaction>>({
                  content: [],
                  totalElements: 0,
                  totalPages: 0,
                  number: 0,
                  size: this.pageSize
                })
              )
            );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((sp) => {
        this.pageData = sp.content ?? [];
        this.filtered = this.pageData;
        this.total = sp.totalElements ?? 0;
        this.tableLoading = false;
        this.cdr.detectChanges();
      });

    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    this.page = 1;
    this.reload$.next();
  }

  resetFilters(): void {
    this.fNo = '';
    this.fSubject = '';
    this.fFrom = '';
    this.fStatus = '';
    this.applyFilters();
  }

  goToPage(p: number): void {
    this.page = p;
    this.reload$.next();
  }

  onSort(ev: { columnId: string; direction: SortDirection }): void {
    this.sortColumn = ev.columnId;
    this.sortDir = ev.direction;
    this.page = 1;
    this.reload$.next();
  }

  onPageSizeChange(n: number): void {
    this.pageSize = n;
    this.page = 1;
    this.reload$.next();
  }

  trackByTxId(_i: number, t: Transaction): string {
    return t.id;
  }

  back(): void {
    this.router.navigate(['/correspondence']);
  }

  calcSla(t: Transaction): number {
    const created = new Date(t.created).getTime();
    const now = new Date().getTime();
    const diffDays = (now - created) / (1000 * 60 * 60 * 24);
    const percent = (diffDays / t.maxDays) * 100;
    return Math.min(percent, 100);
  }

  open(tx: Transaction): void {
    localStorage.setItem('gov-selected-tx', tx.id);
    this.router.navigate(['/correspondence', tx.id]);
  }

  private effectiveStatus(): string | undefined {
    if (this.type === 'ARCHIVED') return 'ARCHIVED';
    if (this.fStatus) return this.fStatus;
    return undefined;
  }

  private effectiveType(): string | undefined {
    if (!this.type || this.type === 'ARCHIVED') return undefined;
    return this.type;
  }

  private composeFreeText(): string | undefined {
    const parts = [this.fNo, this.fSubject, this.fFrom]
      .map((p) => (p ?? '').trim())
      .filter((p) => !!p);
    return parts.length ? parts.join(' ') : undefined;
  }

  /** Map UI sort keys to backend entity properties. */
  private toBackendSort(col: string): string {
    switch (col) {
      case 'id':
        return 'referenceNumber';
      case 'createdAt':
        return 'createdAt';
      case 'subject':
        return 'subject';
      case 'type':
        return 'correspondenceType.code';
      case 'status':
        return 'correspondenceStatus.code';
      case 'sla':
      case 'attachments':
      case 'entity':
      case 'secrecy':
      default:
        return 'createdAt';
    }
  }
}
