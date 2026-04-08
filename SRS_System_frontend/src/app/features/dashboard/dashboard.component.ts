import {
  afterNextRender,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Injector,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { MatIcon } from '@angular/material/icon';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { LookupService } from '../../core/api/lookup.service';
import { DashboardBucketDto, DashboardResponseDto } from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';

export type DashboardRecentRow = {
  id: string;
  referenceNumber: string;
  typeCode: string;
  subject: string;
  created: string;
  statusCode: string;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIcon, TranslatePipe, LookupTranslatePipe, SrsDataTableComponent, SrsSortHeaderComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  animations: [srsTableRowEnter]
})
export class DashboardComponent implements OnInit {
  @ViewChild('statusDonut') statusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deptBar') deptBar!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiStatusDonut') aiStatusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiPriorityBar') aiPriorityBar!: ElementRef<HTMLCanvasElement>;

  /** SLA ring animation — signal updates schedule CD without `detectChanges()`. */
  readonly animatedSlaPercent = signal(0);

  private readonly injector = inject(Injector);

  constructor(
    public router: Router,
    private dashboardApi: DashboardApiService,
    private lookupApi: LookupService,
    private lookupLabels: LookupLabelsService,
    private transactionService: TransactionService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  slaPercent = 0;

  total = 0;
  done = 0;
  inProgress = 0;
  incoming = 0;
  outbound = 0;

  overdueCount = 0;
  slaOnTime = 0;
  slaLate = 0;
  slaAction = 0;

  private dash: DashboardResponseDto | null = null;

  recentLoading = true;
  recentAll: DashboardRecentRow[] = [];
  recentRows: DashboardRecentRow[] = [];
  recentTotal = 0;
  recentPage = 1;
  recentPageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  recentSortColumn = 'created';
  recentSortDir: SortDirection = 'desc';

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.recentLoading = v),
      source: forkJoin({
        dash: this.dashboardApi.getDashboard(),
        recent: this.transactionService.listPage({ page: 0, size: 50 }),
        bundle: this.lookupApi.getBundle().pipe(catchError(() => of(null)))
      }),
      next: ({ dash, recent, bundle }) => {
        if (bundle) {
          this.lookupLabels.hydrateFromBundle(bundle);
        }
        this.dash = dash;
        this.total = dash.totalCorrespondences;
        this.overdueCount = dash.overdueCount;
        this.done = this.countByStatusCodes(dash, ['COMPLETED', 'ARCHIVED']);
        this.inProgress = this.countByStatusCodes(dash, ['IN_PROGRESS', 'PENDING_APPROVAL', 'RETURNED']);
        this.incoming = this.countByStatusCodes(dash, ['NEW']);
        this.outbound = this.countByType(recent, 'OUTBOUND');

        const denom = Math.max(this.total, 1);
        this.slaPercent = Math.min(100, Math.round((this.done / denom) * 100));

        this.slaLate = this.overdueCount;
        this.slaAction = this.inProgress;
        this.slaOnTime = Math.max(0, this.done - this.slaLate);

        this.recentAll = recent.map((t) => ({
          id: t.id,
          referenceNumber: t.referenceNumber ?? t.id,
          typeCode: t.typeCode,
          subject: t.subject,
          created: t.created,
          statusCode: t.statusCode
        }));
        this.rebuildRecentTable();
        afterNextRender(
          () => {
            this.animateSla();
            this.renderCharts();
          },
          { injector: this.injector }
        );
      },
      error: () => {
        this.total = 0;
        this.recentAll = [];
        this.recentRows = [];
        this.recentTotal = 0;
        this.dash = null;
        this.overdueCount = 0;
        this.slaOnTime = 0;
        this.slaLate = 0;
        this.slaAction = 0;
      }
    });
  }

  private countByStatusCodes(d: DashboardResponseDto, codes: string[]): number {
    const set = new Set(codes.map((c) => c.toUpperCase()));
    return (d.byStatus ?? []).filter((b) => set.has(b.code.toUpperCase())).reduce((s, b) => s + b.count, 0);
  }

  private countByType(rows: { typeCode: string }[], code: string): number {
    const u = code.toUpperCase();
    return rows.filter((r) => (r.typeCode ?? '').toUpperCase() === u).length;
  }

  animateSla() {
    let current = 0;
    const step = () => {
      if (current <= this.slaPercent) {
        this.animatedSlaPercent.set(current);
        current++;
        requestAnimationFrame(step);
      }
    };
    step();
  }

  getSlaColor(): string {
    if (this.slaPercent >= 85) {
      return 'linear-gradient(90deg, var(--sla-excellent), var(--gov-primary))';
    }
    if (this.slaPercent >= 70) {
      return 'linear-gradient(90deg, var(--sla-good), var(--gov-primary))';
    }
    if (this.slaPercent >= 50) {
      return 'linear-gradient(90deg, var(--sla-warning), var(--gov-primary))';
    }
    return 'linear-gradient(90deg, var(--sla-danger), var(--gov-primary))';
  }

  private bucketSeries(buckets: DashboardBucketDto[]): { labels: string[]; data: number[] } {
    const lang = this.i18n.currentLang();
    const sorted = [...(buckets ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
    return {
      labels: sorted.map((b) => (lang === 'en' ? b.nameEn : b.nameAr)),
      data: sorted.map((b) => b.count)
    };
  }

  private renderCharts() {
    if (!this.dash) {
      return;
    }

    const status = this.bucketSeries(this.dash.byStatus);
    const priorities = this.bucketSeries(this.dash.byPriority);

    const countAxisLabel = this.i18n.instant('dashboard.chart.count');

    const palette = ['#10B981', '#F59E0B', '#0da1eb', '#6366F1', '#EC4899', '#14B8A6', '#8B5CF6'];

    const doughnutOpts = {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          position: 'bottom' as const,
          rtl: this.i18n.currentLang() === 'ar',
          labels: {
            font: { size: 13, family: 'Cairo, sans-serif', weight: 'bold' as const },
            padding: 15,
            usePointStyle: true,
            pointStyle: 'circle' as const
          }
        }
      }
    };

    if (this.statusDonut?.nativeElement) {
      new Chart(this.statusDonut.nativeElement, {
        type: 'doughnut',
        data: {
          labels: status.labels,
          datasets: [
            {
              data: status.data,
              backgroundColor: status.labels.map((_, i) => palette[i % palette.length]),
              borderColor: '#FFFFFF',
              borderWidth: 3,
              hoverOffset: 10
            }
          ]
        },
        options: doughnutOpts
      });
    }

    if (this.deptBar?.nativeElement) {
      new Chart(this.deptBar.nativeElement, {
        type: 'bar',
        data: {
          labels: priorities.labels,
          datasets: [
            {
              label: countAxisLabel,
              data: priorities.data,
              backgroundColor: priorities.labels.map((_, i) => palette[(i + 2) % palette.length]),
              borderColor: '#FFFFFF',
              borderWidth: 2,
              borderRadius: 8
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: { legend: { display: false } },
          scales: {
            y: { beginAtZero: true },
            x: { grid: { display: false } }
          }
        }
      });
    }

    if (this.aiStatusDonut?.nativeElement) {
      new Chart(this.aiStatusDonut.nativeElement, {
        type: 'doughnut',
        data: {
          labels: status.labels,
          datasets: [
            {
              data: status.data,
              backgroundColor: status.labels.map((_, i) => palette[i % palette.length]),
              borderWidth: 3
            }
          ]
        },
        options: { responsive: true, maintainAspectRatio: true }
      });
    }

    if (this.aiPriorityBar?.nativeElement) {
      new Chart(this.aiPriorityBar.nativeElement, {
        type: 'bar',
        data: {
          labels: priorities.labels,
          datasets: [
            {
              label: this.i18n.instant('dashboard.chart.priorityAxis'),
              data: priorities.data,
              backgroundColor: priorities.labels.map((_, i) => palette[(i + 1) % palette.length]),
              borderRadius: 8
            }
          ]
        },
        options: { responsive: true, maintainAspectRatio: true }
      });
    }
  }

  format(n: number): string {
    const loc = this.i18n.currentLang() === 'en' ? 'en-US' : 'ar-SA';
    return n.toLocaleString(loc);
  }

  openTransctions(): void {
    this.router.navigate(['/transactions']);
  }

  openTransaction(id: string): void {
    this.router.navigate(['/transactions', id]);
  }

  onRecentSort(ev: { columnId: string; direction: SortDirection }): void {
    this.recentSortColumn = ev.columnId;
    this.recentSortDir = ev.direction;
    this.recentPage = 1;
    this.rebuildRecentTable();
  }

  onRecentPage(p: number): void {
    this.recentPage = p;
    this.rebuildRecentTable();
  }

  onRecentPageSize(n: number): void {
    this.recentPageSize = n;
    this.recentPage = 1;
    this.rebuildRecentTable();
  }

  trackByRecentId(_i: number, row: DashboardRecentRow): string {
    return row.id;
  }

  private rebuildRecentTable(): void {
    const sorted = this.sortRecentRows([...this.recentAll]);
    const r = srsClientPaginate(sorted, this.recentPage, this.recentPageSize);
    this.recentPage = r.page;
    this.recentTotal = r.total;
    this.recentRows = r.pageRows;
  }

  private sortRecentRows(rows: DashboardRecentRow[]): DashboardRecentRow[] {
    const col = this.recentSortColumn;
    const dir = this.recentSortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'ref':
          return compareSortValues(a.referenceNumber, b.referenceNumber, dir);
        case 'type':
          return compareSortValues(a.typeCode, b.typeCode, dir);
        case 'subject':
          return compareSortValues(a.subject, b.subject, dir);
        case 'created': {
          const ta = new Date(a.created).getTime();
          const tb = new Date(b.created).getTime();
          const na = Number.isNaN(ta) ? 0 : ta;
          const nb = Number.isNaN(tb) ? 0 : tb;
          return compareSortValues(na, nb, dir);
        }
        case 'status':
          return compareSortValues(a.statusCode, b.statusCode, dir);
        default:
          return 0;
      }
    });
  }
}
