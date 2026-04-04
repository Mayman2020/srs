import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Transaction } from '../../models/transaction.model';
import { TransactionService } from '../../services/transaction.service';
import { CreateTransactionButton } from '../create-transaction/create-transaction-button/create-transaction-button';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { LookupService } from '../../core/api/lookup.service';
import { LookupItemDto } from '../../core/api/api-types';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CreateTransactionButton,
    TranslatePipe,
    LookupTranslatePipe
  ],
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.css']
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
  pageSize = 5;
  total = 0;

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
    public router: Router
  ) {}

  ngOnInit(): void {
    forkJoin({
      list: this.service.listPage(),
      dash: this.dashboardApi.getSummary(),
      lookups: this.lookupService.getBundle()
    }).subscribe({
      next: ({ list, dash, lookups }) => {
        this.loadError = false;
        this.all = list;
        this.dashTotal = dash.totalCorrespondence;
        this.dashInbound = dash.inboundCount;
        this.dashOutbound = dash.outboundCount;
        this.dashInProgress = dash.inProgressCount;
        this.dashCompleted = dash.completedCount;
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
      const subject = (t.subject ?? '').toLowerCase();
      const from = (t.from ?? '').toLowerCase();

      if (fNo && !id.includes(fNo)) return false;
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
    this.total = this.filtered.length;
    const start = (this.page - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pageData = this.filtered.slice(start, end);
  }

  goToPage(p: number): void {
    this.page = p;
    this.applyPagination();
  }

  next(): void {
    if (this.page < this.pages().length) {
      this.page++;
      this.applyPagination();
    }
  }

  prev(): void {
    if (this.page > 1) {
      this.page--;
      this.applyPagination();
    }
  }

  changeSize(): void {
    this.page = 1;
    this.applyPagination();
  }

  pages(): number[] {
    const count = Math.ceil(this.total / this.pageSize);
    return Array.from({ length: count }, (_, i) => i + 1);
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
