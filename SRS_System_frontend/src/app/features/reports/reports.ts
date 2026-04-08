import {
  ChangeDetectorRef,
  Component,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, debounceTime, skip } from 'rxjs/operators';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { LookupService } from '../../core/api/lookup.service';
import { ReportsApiService } from '../../core/api/reports-api.service';
import { CorrespondenceListParams } from '../../core/api/correspondence-api.service';
import {
  DashboardBucketDto,
  DashboardResponseDto,
  DepartmentSlaRowDto,
  LookupItemDto,
  ReportMonthlyPointDto
} from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../models/transaction.model';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { SrsFilterBarComponent } from '../../shared/data-table/srs-filter-bar.component';
import { SrsEmptyStateComponent } from '../../shared/data-table/srs-empty-state.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import type { SortDirection } from '../../shared/data-table/table-sort.util';
import { REPORT_TABLE_SORT_PROPERTY, reportSpringSort } from './report-table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    LookupTranslatePipe,
    MatSnackBarModule,
    SrsDataTableComponent,
    SrsSortHeaderComponent,
    SrsFilterBarComponent,
    SrsEmptyStateComponent
  ],
  templateUrl: './reports.html',
  styleUrls: ['./reports.css'],
  animations: [srsTableRowEnter]
})
export class ReportsComponent implements OnInit, AfterViewInit, OnDestroy {
  reportForm: FormGroup;

  @ViewChild('trendCanvas', { static: false }) trendCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusCanvas', { static: false }) statusCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deptBarCanvas', { static: false }) deptBarCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('priorityCanvas', { static: false }) priorityCanvas!: ElementRef<HTMLCanvasElement>;

  trendChart!: Chart;
  statusChart!: Chart;
  deptBarChart!: Chart;
  priorityChart!: Chart;

  kpis = {
    total: 0,
    done: 0,
    late: 0,
    avg: 0
  };

  /** True when KPI slice is capped below total matching rows (server max fetch). */
  statsTruncated = false;

  correspondenceTypes: LookupItemDto[] = [];
  correspondenceStatuses: LookupItemDto[] = [];

  reportRows: Transaction[] = [];
  reportTotal = 0;
  reportPage = 1;
  reportPageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  reportSortUiColumn = 'created';
  reportSortUiDir: SortDirection = 'desc';
  tableLoading = false;
  private tableHasData = false;

  private dash: DashboardResponseDto | null = null;
  private chartsReady = false;

  private apiStatus: DashboardBucketDto[] = [];
  private apiPriority: DashboardBucketDto[] = [];
  private apiMonthly: ReportMonthlyPointDto[] = [];
  private apiDept: DepartmentSlaRowDto[] = [];

  private readonly statsCap = 2000;

  constructor(
    private fb: FormBuilder,
    private dashboardApi: DashboardApiService,
    private transactionService: TransactionService,
    private lookupService: LookupService,
    private reportsApi: ReportsApiService,
    private i18n: I18nService,
    private snackBar: MatSnackBar,
    private lookupLabels: LookupLabelsService,
    private router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.reportForm = this.fb.group({
      from: [''],
      to: [''],
      type: [''],
      status: ['']
    });
  }

  get reportSortParams(): string[] {
    return reportSpringSort(this.reportSortUiColumn, this.reportSortUiDir);
  }

  get tableLoadingMode(): 'skeleton' | 'overlay' {
    return this.tableHasData ? 'overlay' : 'skeleton';
  }

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: forkJoin({
        dash: this.dashboardApi.getDashboard(),
        lookups: this.lookupService.getBundle(),
        st: this.reportsApi.statusDistribution(),
        pr: this.reportsApi.priorityDistribution(),
        mo: this.reportsApi.monthlyTrend(),
        dep: this.reportsApi.departmentSlaHeatmap()
      }),
      next: ({ dash, lookups, st, pr, mo, dep }) => {
        this.dash = dash;
        this.apiStatus = st ?? [];
        this.apiPriority = pr ?? [];
        this.apiMonthly = mo ?? [];
        this.apiDept = dep ?? [];
        this.lookupLabels.hydrateFromBundle(lookups);
        this.correspondenceTypes = lookups.correspondenceTypes ?? [];
        this.correspondenceStatuses = lookups.correspondenceStatuses ?? [];
        this.kpis.total = dash.totalCorrespondences;
        this.kpis.late = dash.overdueCount;
        this.kpis.done = (dash.byStatus ?? [])
          .filter((b) => ['COMPLETED', 'ARCHIVED'].includes(b.code.toUpperCase()))
          .reduce((s, b) => s + b.count, 0);
        this.kpis.avg = 0;
        if (this.chartsReady) {
          this.renderCharts();
        }
        this.loadStatsAndTable();
      },
      error: () => {
        this.dash = null;
        this.apiStatus = [];
        this.apiPriority = [];
        this.apiMonthly = [];
        this.apiDept = [];
        this.kpis = { total: 0, done: 0, late: 0, avg: 0 };
        this.reportRows = [];
        this.reportTotal = 0;
      }
    });

    this.reportForm.valueChanges
      .pipe(skip(1), debounceTime(400))
      .subscribe(() => {
        this.reportPage = 1;
        this.loadStatsAndTable();
        this.reloadMonthlyFromForm();
      });
  }

  ngAfterViewInit(): void {
    this.chartsReady = true;
    if (this.dash) {
      this.renderCharts();
    }
  }

  ngOnDestroy(): void {
    this.trendChart?.destroy();
    this.statusChart?.destroy();
    this.deptBarChart?.destroy();
    this.priorityChart?.destroy();
  }

  runReport(): void {
    this.reportPage = 1;
    this.loadStatsAndTable();
    this.reloadMonthlyFromForm();
  }

  clearReportFilters(): void {
    this.reportForm.setValue({ from: '', to: '', type: '', status: '' });
    this.reportPage = 1;
    this.loadStatsAndTable();
    this.reloadMonthlyFromForm();
  }

  onReportSort(ev: { columnId: string; direction: SortDirection }): void {
    if (!REPORT_TABLE_SORT_PROPERTY[ev.columnId]) {
      return;
    }
    this.reportSortUiColumn = ev.columnId;
    this.reportSortUiDir = ev.direction;
    this.reportPage = 1;
    this.loadTableOnly();
  }

  onReportPage(p: number): void {
    this.reportPage = p;
    this.loadTableOnly();
  }

  onReportPageSize(n: number): void {
    this.reportPageSize = n;
    this.reportPage = 1;
    this.loadTableOnly();
  }

  trackByReportTx(_i: number, t: Transaction): string {
    return t.id;
  }

  openReportRow(tx: Transaction): void {
    localStorage.setItem('gov-selected-tx', tx.id);
    this.router.navigate(['/transactions', tx.id]);
  }

  private buildFilterParams(): Omit<CorrespondenceListParams, 'page' | 'size' | 'sort'> {
    const { from, to, type, status } = this.reportForm.value;
    const p: Omit<CorrespondenceListParams, 'page' | 'size' | 'sort'> = {};
    if (type) {
      p.type = type;
    }
    if (status) {
      p.status = status;
    }
    if (from) {
      p.createdFrom = new Date(from + 'T00:00:00.000Z').toISOString();
    }
    if (to) {
      const d = new Date(to + 'T23:59:59.999Z');
      p.createdTo = d.toISOString();
    }
    return p;
  }

  /** Stats + first page (filters / sort changes). */
  private loadStatsAndTable(): void {
    const filters = this.buildFilterParams();
    const sort = this.reportSortParams;

    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.tableLoading = v),
      source: forkJoin({
        stats: this.transactionService.fetchMatchingUpTo(filters, this.statsCap, sort).pipe(
          catchError(() => of([] as Transaction[]))
        ),
        page: this.transactionService
          .listSpringPage({
            ...filters,
            page: this.reportPage - 1,
            size: this.reportPageSize,
            sort
          })
          .pipe(catchError(() => of(null)))
      }),
      next: ({ stats, page }) => {
        const statsRows = stats ?? [];
        const totalEl = page?.totalElements ?? statsRows.length;
        this.reportTotal = totalEl;
        this.statsTruncated = totalEl > statsRows.length;
        if (page) {
          this.reportRows = [...(page.content ?? [])];
          this.tableHasData = true;
        } else {
          this.reportRows = [];
        }
        this.applyKpisFromStats(statsRows, totalEl);
      }
    });
  }

  /** Paging only (sort changes reset page and call loadStatsAndTable from caller). */
  private loadTableOnly(): void {
    const filters = this.buildFilterParams();
    const sort = this.reportSortParams;

    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.tableLoading = v),
      source: this.transactionService
        .listSpringPage({
          ...filters,
          page: this.reportPage - 1,
          size: this.reportPageSize,
          sort
        })
        .pipe(catchError(() => of(null))),
      next: (page) => {
        if (page) {
          this.reportRows = [...(page.content ?? [])];
          this.reportTotal = page.totalElements ?? 0;
          this.tableHasData = true;
        } else {
          this.reportRows = [];
          this.reportTotal = 0;
        }
      }
    });
  }

  private applyKpisFromStats(rows: Transaction[], total: number): void {
    this.kpis.total = total;
    this.kpis.done = rows.filter((t) =>
      ['COMPLETED', 'ARCHIVED'].includes((t.statusCode ?? '').toUpperCase())
    ).length;
    this.kpis.late = rows.filter(
      (t) =>
        t.dueDateIso &&
        new Date(t.dueDateIso) < new Date() &&
        !['COMPLETED', 'ARCHIVED'].includes((t.statusCode ?? '').toUpperCase())
    ).length;
    this.kpis.avg = this.avgResolutionDays(rows);
  }

  private reloadMonthlyFromForm(): void {
    const { from, to } = this.reportForm.value;
    const fromIso = from ? new Date(from).toISOString() : undefined;
    const toIso = to ? new Date(to).toISOString() : undefined;
    subscribePageLoad({
      cdr: this.cdr,
      source: this.reportsApi.monthlyTrend(fromIso, toIso),
      next: (mo) => {
        this.apiMonthly = mo ?? [];
        this.renderCharts();
      },
      error: () => {
        this.apiMonthly = [];
        this.renderCharts();
      }
    });
  }

  private avgResolutionDays(rows: Transaction[]): number {
    if (!rows.length) {
      return 0;
    }
    const done = rows.filter((t) =>
      ['COMPLETED', 'ARCHIVED'].includes((t.statusCode ?? '').toUpperCase())
    );
    if (!done.length) {
      return 0;
    }
    let sum = 0;
    for (const t of done) {
      const c0 = new Date(t.created).getTime();
      sum += Math.max(0, (Date.now() - c0) / 86_400_000);
    }
    return Math.round(sum / done.length);
  }

  private renderCharts(): void {
    if (
      !this.chartsReady ||
      !this.trendCanvas?.nativeElement ||
      !this.statusCanvas?.nativeElement ||
      !this.deptBarCanvas?.nativeElement ||
      !this.priorityCanvas?.nativeElement
    ) {
      return;
    }

    const lang = this.i18n.currentLang();

    const monthly = [...this.apiMonthly].sort((a, b) => a.period.localeCompare(b.period));
    const trendLabels = monthly.length ? monthly.map((m) => m.period) : ['—'];
    const trendData = monthly.length ? monthly.map((m) => m.count) : [0];

    this.trendChart?.destroy();
    this.trendChart = new Chart(this.trendCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: trendLabels,
        datasets: [
          {
            label: this.i18n.instant('reports.chartMonthlyCreated'),
            data: trendData,
            borderColor: '#0f6b4d',
            backgroundColor: 'rgba(15,107,77,0.15)',
            tension: 0.4,
            fill: true,
            pointRadius: 5
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });

    const statusBuckets = this.apiStatus.length ? this.apiStatus : (this.dash?.byStatus ?? []);
    const sortedStatus = [...statusBuckets].sort((a, b) => a.sortOrder - b.sortOrder);
    const statusLabels = sortedStatus.length
      ? sortedStatus.map((b) => (lang === 'en' ? b.nameEn : b.nameAr))
      : ['—'];
    const statusData = sortedStatus.length ? sortedStatus.map((b) => b.count) : [0];

    this.statusChart?.destroy();
    this.statusChart = new Chart(this.statusCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: statusLabels,
        datasets: [
          {
            data: statusData,
            backgroundColor: ['#1b7f5e', '#0f6b4d', '#f59e0b', '#22c55e', '#6366f1', '#ec4899']
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });

    const pri = this.apiPriority.length ? this.apiPriority : (this.dash?.byPriority ?? []);
    const sortedPri = [...pri].sort((a, b) => a.sortOrder - b.sortOrder);
    const priLabels = sortedPri.length
      ? sortedPri.map((b) => (lang === 'en' ? b.nameEn : b.nameAr))
      : ['—'];
    const priData = sortedPri.length ? sortedPri.map((b) => b.count) : [0];

    this.priorityChart?.destroy();
    this.priorityChart = new Chart(this.priorityCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: priLabels,
        datasets: [
          {
            data: priData,
            backgroundColor: ['#1b7f5e', '#0f6b4d', '#f59e0b', '#22c55e', '#6366f1', '#ec4899']
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });

    this.deptBarChart?.destroy();
    const deptRows = this.apiDept ?? [];
    const deptLabels = deptRows.length
      ? deptRows.map((d) => (lang === 'en' ? d.nameEn : d.nameAr))
      : ['—'];
    const deptOverdue = deptRows.length ? deptRows.map((d) => d.overdueOpen) : [0];

    this.deptBarChart = new Chart(this.deptBarCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: deptLabels,
        datasets: [
          {
            label: this.i18n.instant('reports.chartDeptOverdue'),
            data: deptOverdue,
            backgroundColor: '#f59e0b'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }

  exportPdf(): void {
    window.print();
  }

  exportExcel(): void {
    this.reportsApi.exportExcelBlob().subscribe({
      next: (blob) => {
        if (blob.type?.includes('json')) {
          blob.text().then(() => {
            this.snackBar.open(
              this.i18n.instant('reports.exportExcelError'),
              this.i18n.instant('common.close'),
              { duration: 5000 }
            );
          });
          return;
        }
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'correspondences-export.xlsx';
        a.click();
        URL.revokeObjectURL(url);
        this.snackBar.open(
          this.i18n.instant('reports.exportExcelSuccess'),
          this.i18n.instant('common.close'),
          { duration: 3000 }
        );
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.snackBar.open(
          err.userMessage ?? this.i18n.instant('reports.exportExcelError'),
          this.i18n.instant('common.close'),
          { duration: 5000 }
        );
      }
    });
  }

  lookupTypeLabel(code: string): string {
    const row = this.correspondenceTypes.find((x) => x.code === code);
    if (!row) {
      return code;
    }
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  lookupStatusLabel(code: string): string {
    const row = this.correspondenceStatuses.find((x) => x.code === code);
    if (!row) {
      return code;
    }
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }
}
