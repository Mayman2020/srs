import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Transaction } from '../../models/transaction.model';
import { TransactionService } from '../../services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';

@Component({
  selector: 'app-transactions-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, LookupTranslatePipe],
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.css']
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
  pageSize = 5;
  total = 0;

  statusFilterCodes: string[] = [];

  constructor(
    private service: TransactionService,
    private route: ActivatedRoute,
    public router: Router,
    private lookupLabels: LookupLabelsService,
    private i18n: I18nService
  ) {
    this.statusFilterCodes = this.lookupLabels.orderedRows('correspondenceStatus').map((r) => r.code);
  }

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
    this.service.listPage().subscribe({
      next: (list) => {
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

  applyPagination(): void {
    this.total = this.filtered.length;
    const start = (this.page - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.pageData = this.filtered.slice(start, end);
  }

  pages(): number[] {
    const count = Math.ceil(this.total / this.pageSize);
    return Array.from({ length: count }, (_, i) => i + 1);
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
