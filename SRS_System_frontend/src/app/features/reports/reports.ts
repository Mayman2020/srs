import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, debounceTime, skip } from 'rxjs/operators';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { ReportsApiService } from '../../core/api/reports-api.service';
import { CorrespondenceListParams } from '../../core/api/correspondence-api.service';
import {
  CorrespondenceKpiSegment,
  DashboardBucketDto,
  DashboardResponseDto,
  DepartmentSlaRowDto,
  LookupItemDto,
  ReportMonthlyPointDto
} from '../../core/api/api-types';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model';
import { I18nService } from '../../core/i18n/i18n.service';
import { UiFormatService } from '../../core/i18n/ui-format.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpErrorResponse } from '@angular/common/http';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { SrsFilterBarComponent } from '../../shared/data-table/srs-filter-bar.component';
import { SrsEmptyStateComponent } from '../../shared/data-table/srs-empty-state.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import type { SortDirection } from '../../shared/data-table/table-sort.util';
import { REPORT_TABLE_SORT_PROPERTY, reportSpringSort } from './report-table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { NotificationService } from '../../core/services/notification.service';
import { ThemeService } from '../../core/services/theme.service';

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    LookupTranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    SrsDataTableComponent,
    SrsSortHeaderComponent,
    SrsFilterBarComponent,
    SrsEmptyStateComponent,
    StatusBadgeComponent,
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
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private fb: FormBuilder,
    private dashboardApi: DashboardApiService,
    private transactionService: TransactionService,
    private reportsApi: ReportsApiService,
    private i18n: I18nService,
    private format: UiFormatService,
    private lookupLabels: LookupLabelsService,
    private notification: NotificationService,
    private theme: ThemeService,
    private router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.reportForm = this.fb.group({
      from: [''],
      to: [''],
      type: [''],
      status: ['']
    });

    this.theme.mode$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.chartsReady) {
          this.renderCharts();
        }
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
        correspondenceTypes: this.lookupLabels.loadTable(LookupCode.CorrespondenceType),
        correspondenceStatuses: this.lookupLabels.loadTable(LookupCode.CorrespondenceStatus),
        st: this.reportsApi.statusDistribution(),
        pr: this.reportsApi.priorityDistribution(),
        mo: this.reportsApi.monthlyTrend(),
        dep: this.reportsApi.departmentSlaHeatmap()
      }),
      next: ({ dash, correspondenceTypes, correspondenceStatuses, st, pr, mo, dep }) => {
        this.dash = dash;
        this.apiStatus = st ?? [];
        this.apiPriority = pr ?? [];
        this.apiMonthly = mo ?? [];
        this.apiDept = dep ?? [];
        this.correspondenceTypes = correspondenceTypes ?? [];
        this.correspondenceStatuses = correspondenceStatuses ?? [];
        this.kpis.total = dash.totalCorrespondences;
        this.kpis.late = dash.overdueCount;
        this.kpis.done = dash.kpiSlaDoneCount ?? 0;
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
    this.router.navigate(['/correspondence', tx.id]);
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
    this.kpis.done = rows.filter((t) => this.statusIsSlaDoneSegment(t.statusCode)).length;
    this.kpis.late = rows.filter(
      (t) =>
        t.dueDateIso &&
        new Date(t.dueDateIso) < new Date() &&
        !this.statusIsSlaDoneSegment(t.statusCode)
    ).length;
    this.kpis.avg = this.avgResolutionDays(rows);
  }

  /** Aligns with `correspondence_status.kpi_segment = SLA_DONE` (see lookup bundle). */
  private statusIsSlaDoneSegment(code: string | undefined): boolean {
    const c = (code ?? '').trim();
    if (!c) return false;
    const row = this.correspondenceStatuses.find(
      (s) => (s.code ?? '').toUpperCase() === c.toUpperCase()
    );
    return row?.kpiSegment === CorrespondenceKpiSegment.SLA_DONE;
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
    const done = rows.filter((t) => this.statusIsSlaDoneSegment(t.statusCode));
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
    const colors = this.chartColors();

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
            borderColor: colors.primary,
            backgroundColor: colors.primarySoft,
            tension: 0.4,
            fill: true,
            pointRadius: 5
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: colors.text } } },
        scales: {
          x: { ticks: { color: colors.text }, grid: { color: colors.grid } },
          y: { ticks: { color: colors.text }, grid: { color: colors.grid } }
        }
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
            backgroundColor: [colors.primary, '#0f6b4d', colors.warning, '#22c55e', '#6366f1', '#ec4899'],
            borderColor: colors.surface
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: colors.text } } }
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
            backgroundColor: [colors.primary, '#0f6b4d', colors.warning, '#22c55e', '#6366f1', '#ec4899'],
            borderColor: colors.surface
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: colors.text } } }
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
            backgroundColor: colors.warning
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: colors.text }
          }
        },
        scales: {
          x: { ticks: { color: colors.text }, grid: { display: false } },
          y: { ticks: { color: colors.text }, grid: { color: colors.grid } }
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
            this.notification.errorRaw(this.i18n.instant('reports.exportExcelError'));
          });
          return;
        }
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'correspondences-export.xlsx';
        a.click();
        URL.revokeObjectURL(url);
        this.notification.successRaw(this.i18n.instant('reports.exportExcelSuccess'));
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.notification.errorRaw(err.userMessage ?? this.i18n.instant('reports.exportExcelError'));
      }
    });
  }

  formatMetric(value: number): string {
    return this.format.formatNumber(value);
  }

  formatCreated(value: string): string {
    return this.format.formatDate(value, 'd MMM y - hh:mm a');
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

  private chartColors(): {
    primary: string;
    primarySoft: string;
    warning: string;
    text: string;
    grid: string;
    surface: string;
  } {
    const styles = getComputedStyle(document.documentElement);
    return {
      primary: styles.getPropertyValue('--primary-color').trim() || '#0b6e4f',
      primarySoft:
        styles.getPropertyValue('--primary-light').trim() || 'rgba(11,110,79,0.18)',
      warning: styles.getPropertyValue('--warning-color').trim() || '#d97706',
      text: styles.getPropertyValue('--text-secondary').trim() || '#475569',
      grid: styles.getPropertyValue('--border-color').trim() || '#d9e3ef',
      surface: styles.getPropertyValue('--surface-elevated').trim() || '#ffffff'
    };
  }
}
