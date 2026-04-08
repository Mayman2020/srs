import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

import { Transaction } from '../../models/transaction.model';
import { TransactionService } from '../../services/transaction.service';
import { CreateTransactionButton } from '../create-transaction/create-transaction-button/create-transaction-button';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { LookupService } from '../../core/api/lookup.service';
import { LookupItemDto } from '../../core/api/api-types';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CreateTransactionButton,
    TranslatePipe,
    LookupTranslatePipe,
    SrsDataTableComponent,
    SrsSortHeaderComponent
  ],
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.css'],
  animations: [srsTableRowEnter]
})
export class TransactionsComponent implements OnInit {
  all: Transaction[] = [];
  filtered: Transaction[] = [];
  pageData: Transaction[] = [];

  fNo = '';
  fSubject = '';
  fFrom = '';
  fType = '';
  fStatus = '';

  page = 1;
  pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  total = 0;

  tableLoading = true;
  sortColumn = 'id';
  sortDir: SortDirection = 'asc';

  correspondenceTypes: LookupItemDto[] = [];
  correspondenceStatuses: LookupItemDto[] = [];

  dashTotal = 0;
  dashInbound = 0;
  dashOutbound = 0;
  dashInProgress = 0;
  dashCompleted = 0;

  loadError = false;

  constructor(
    private service: TransactionService,
    private dashboardApi: DashboardApiService,
    private lookupService: LookupService,
    private lookupLabels: LookupLabelsService,
    public router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.tableLoading = v),
      source: forkJoin({
        list: this.service.listPage(),
        dash: this.dashboardApi.getDashboard(),
        lookups: this.lookupService.getBundle()
      }),
      next: ({ list, dash, lookups }) => {
        this.loadError = false;
        this.lookupLabels.hydrateFromBundle(lookups);
        this.all = list;
        this.dashTotal = dash.totalCorrespondences;
        const inbound = list.filter((t) => (t.typeCode ?? '').toUpperCase() === 'INBOUND').length;
        const outbound = list.filter((t) => (t.typeCode ?? '').toUpperCase() === 'OUTBOUND').length;
        this.dashInbound = inbound;
        this.dashOutbound = outbound;
        this.dashInProgress = (dash.byStatus ?? [])
          .filter((b) => ['IN_PROGRESS', 'PENDING_APPROVAL', 'RETURNED'].includes(b.code.toUpperCase()))
          .reduce((s, b) => s + b.count, 0);
        this.dashCompleted = (dash.byStatus ?? [])
          .filter((b) => ['ARCHIVED', 'COMPLETED'].includes(b.code.toUpperCase()))
          .reduce((s, b) => s + b.count, 0);
        this.correspondenceTypes = lookups.correspondenceTypes;
        this.correspondenceStatuses = lookups.correspondenceStatuses;
        this.applyFilters();
      },
      error: () => {
        this.loadError = true;
        this.all = [];
        this.applyFilters();
      }
    });
  }

  applyFilters(): void {
    const fNo = (this.fNo || '').toLowerCase().trim();
    const fSubject = (this.fSubject || '').toLowerCase().trim();
    const fFrom = (this.fFrom || '').toLowerCase().trim();

    this.filtered = this.all.filter((t) => {
      const id = (t.id ?? '').toString().toLowerCase();
      const ref = (t.referenceNumber ?? '').toString().toLowerCase();
      const subject = (t.subject ?? '').toLowerCase();
      const from = (t.from ?? '').toLowerCase();

      if (fNo && !id.includes(fNo) && !ref.includes(fNo)) return false;
      if (fSubject && !subject.includes(fSubject)) return false;
      if (fFrom && !from.includes(fFrom)) return false;
      if (this.fType && t.typeCode !== this.fType) return false;
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
    this.fType = '';
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

  goToPage(p: number): void {
    this.page = p;
    this.applyPagination();
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

  openType(typeCode: string): void {
    this.router.navigate(['/transactions/list', typeCode]);
  }
}
