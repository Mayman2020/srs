import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

import { Transaction } from '../../models/transaction.model';
import { TransactionService } from '../../services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupService } from '../../core/api/lookup.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';

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
export class TransactionsListComponent implements OnInit {
  all: Transaction[] = [];
  filtered: Transaction[] = [];
  pageData: Transaction[] = [];

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

  constructor(
    private service: TransactionService,
    private route: ActivatedRoute,
    public router: Router,
    private lookupLabels: LookupLabelsService,
    private lookupApi: LookupService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  get listContextLabel(): string {
    if (!this.type) {
      return this.i18n.instant('common.all');
    }
    if (this.type === 'ARCHIVED') {
      return this.lookupLabels.label('correspondenceStatus', 'ARCHIVED');
    }
    return this.lookupLabels.label('correspondenceType', this.type);
  }

  ngOnInit(): void {
    this.type = this.route.snapshot.paramMap.get('type') || '';
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.tableLoading = v),
      source: forkJoin({
        bundle: this.lookupApi.getBundle().pipe(catchError(() => of(null))),
        list: this.service.listPage().pipe(catchError(() => of([] as Transaction[])))
      }),
      next: ({ bundle, list }) => {
        if (bundle) {
          this.lookupLabels.hydrateFromBundle(bundle);
          this.statusFilterCodes = this.lookupLabels
            .orderedRows('correspondenceStatus')
            .map((r) => r.code);
        }
        if (this.type === 'ARCHIVED') {
          this.all = list.filter((t) => t.statusCode === 'ARCHIVED');
        } else if (this.type) {
          this.all = list.filter((t) => t.typeCode === this.type);
        } else {
          this.all = list;
        }
        this.applyFilters();
      },
      error: () => {
        this.all = [];
        this.applyFilters();
      }
    });
  }

  applyFilters(): void {
    this.filtered = this.all.filter((t) => {
      const id = t.id?.toString().toLowerCase() ?? '';
      const ref = (t.referenceNumber ?? '').toString().toLowerCase();
      const subject = t.subject?.toLowerCase() ?? '';
      const from = t.from?.toLowerCase() ?? '';

      if (this.fNo && !id.includes(this.fNo.toLowerCase()) && !ref.includes(this.fNo.toLowerCase()))
        return false;
      if (this.fSubject && !subject.includes(this.fSubject.toLowerCase())) return false;
      if (this.fFrom && !from.includes(this.fFrom.toLowerCase())) return false;
      if (this.fStatus && t.statusCode !== this.fStatus) return false;
      return true;
    });

    this.page = 1;
    this.applyPagination();
  }

  resetFilters(): void {
    this.fNo = '';
    this.fSubject = '';
    this.fFrom = '';
    this.fStatus = '';
    this.applyFilters();
  }

  applyPagination(): void {
    const sorted = this.sortTransactions(this.filtered);
    const r = srsClientPaginate(sorted, this.page, this.pageSize);
    this.page = r.page;
    this.total = r.total;
    this.pageData = r.pageRows;
  }

  goToPage(p: number): void {
    this.page = p;
    this.applyPagination();
  }

  onSort(ev: { columnId: string; direction: SortDirection }): void {
    this.sortColumn = ev.columnId;
    this.sortDir = ev.direction;
    this.page = 1;
    this.applyPagination();
  }

  onPageSizeChange(n: number): void {
    this.pageSize = n;
    this.page = 1;
    this.applyPagination();
  }

  trackByTxId(_i: number, t: Transaction): string {
    return t.id;
  }

  private sortTransactions(rows: Transaction[]): Transaction[] {
    const col = this.sortColumn;
    const dir = this.sortDir;
    return [...rows].sort((a, b) => {
      switch (col) {
        case 'createdAt':
          return compareSortValues(a.createdAt.getTime(), b.createdAt.getTime(), dir);
        case 'id': {
          const na = Number(a.id);
          const nb = Number(b.id);
          if (!Number.isNaN(na) && !Number.isNaN(nb)) {
            return compareSortValues(na, nb, dir);
          }
          return compareSortValues(a.id, b.id, dir);
        }
        case 'type':
          return compareSortValues(a.typeCode, b.typeCode, dir);
        case 'subject':
          return compareSortValues(a.subject, b.subject, dir);
        case 'entity': {
          const sa = `${a.from ?? ''} ${a.to ?? ''}`;
          const sb = `${b.from ?? ''} ${b.to ?? ''}`;
          return compareSortValues(sa, sb, dir);
        }
        case 'sla':
          return compareSortValues(this.calcSla(a), this.calcSla(b), dir);
        case 'secrecy':
          return compareSortValues(a.secrecy || '', b.secrecy || '', dir);
        case 'attachments':
          return compareSortValues(a.attachments?.length ?? 0, b.attachments?.length ?? 0, dir);
        case 'status':
          return compareSortValues(a.statusCode, b.statusCode, dir);
        default:
          return 0;
      }
    });
  }

  back(): void {
    this.router.navigate(['/transactions']);
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
    this.router.navigate(['/transactions', tx.id]);
  }
}
