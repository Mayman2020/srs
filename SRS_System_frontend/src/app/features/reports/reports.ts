import {
  Component,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { forkJoin } from 'rxjs';
import { skip } from 'rxjs/operators';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { LookupService } from '../../core/api/lookup.service';
import { ReportsApiService } from '../../core/api/reports-api.service';
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

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './reports.html',
  styleUrls: ['./reports.css']
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

  correspondenceTypes: LookupItemDto[] = [];
  correspondenceStatuses: LookupItemDto[] = [];

  private dash: DashboardResponseDto | null = null;
  private list: Transaction[] = [];
  private chartsReady = false;

  private apiStatus: DashboardBucketDto[] = [];
  private apiPriority: DashboardBucketDto[] = [];
  private apiMonthly: ReportMonthlyPointDto[] = [];
  private apiDept: DepartmentSlaRowDto[] = [];

  constructor(
    private fb: FormBuilder,
    private dashboardApi: DashboardApiService,
    private transactionService: TransactionService,
    private lookupService: LookupService,
    private reportsApi: ReportsApiService,
    private i18n: I18nService,
    private cdr: ChangeDetectorRef
  ) {
    this.reportForm = this.fb.group({
      from: [''],
      to: [''],
      type: [''],
      status: ['']
    });
  }

  ngOnInit(): void {
    forkJoin({
      dash: this.dashboardApi.getDashboard(),
      list: this.transactionService.listPage({ page: 0, size: 2000 }),
      lookups: this.lookupService.getBundle(),
      st: this.reportsApi.statusDistribution(),
      pr: this.reportsApi.priorityDistribution(),
      mo: this.reportsApi.monthlyTrend(),
      dep: this.reportsApi.departmentSlaHeatmap()
    }).subscribe({
      next: ({ dash, list, lookups, st, pr, mo, dep }) => {
        this.dash = dash;
        this.list = list;
        this.apiStatus = st ?? [];
        this.apiPriority = pr ?? [];
        this.apiMonthly = mo ?? [];
        this.apiDept = dep ?? [];
        this.correspondenceTypes = lookups.correspondenceTypes ?? [];
        this.correspondenceStatuses = lookups.correspondenceStatuses ?? [];
        this.kpis.total = dash.totalCorrespondences;
        this.kpis.late = dash.overdueCount;
        this.kpis.done = (dash.byStatus ?? [])
          .filter((b) => ['COMPLETED', 'ARCHIVED'].includes(b.code.toUpperCase()))
          .reduce((s, b) => s + b.count, 0);
        this.kpis.avg = this.avgResolutionDays(list);
        this.cdr.detectChanges();
        if (this.chartsReady) {
          this.renderCharts();
        }
      },
      error: () => {
        this.dash = null;
        this.list = [];
        this.apiStatus = [];
        this.apiPriority = [];
        this.apiMonthly = [];
        this.apiDept = [];
        this.kpis = { total: 0, done: 0, late: 0, avg: 0 };
      }
    });

    this.reportForm.valueChanges.pipe(skip(1)).subscribe(() => {
      this.runReport();
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

  private filteredList(): Transaction[] {
    const { from, to, type, status } = this.reportForm.value;
    const fromMs = from ? new Date(from).getTime() : null;
    const toMs = to ? new Date(to).getTime() + 86_400_000 - 1 : null;
    return this.list.filter((t) => {
      const created = new Date(t.created).getTime();
      if (fromMs !== null && created < fromMs) {
        return false;
      }
      if (toMs !== null && created > toMs) {
        return false;
      }
      if (type && t.typeCode !== type) {
        return false;
      }
      if (status && t.statusCode !== status) {
        return false;
      }
      return true;
    });
  }

  private reloadMonthlyFromForm(): void {
    const { from, to } = this.reportForm.value;
    const fromIso = from ? new Date(from).toISOString() : undefined;
    const toIso = to ? new Date(to).toISOString() : undefined;
    this.reportsApi.monthlyTrend(fromIso, toIso).subscribe({
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

  runReport() {
    const rows = this.filteredList();
    this.kpis.total = rows.length;
    this.kpis.done = rows.filter((t) =>
      ['COMPLETED', 'ARCHIVED'].includes((t.statusCode ?? '').toUpperCase())
    ).length;
    this.kpis.late = rows.filter(
      (t) => t.dueDateIso && new Date(t.dueDateIso) < new Date() && !['COMPLETED', 'ARCHIVED'].includes((t.statusCode ?? '').toUpperCase())
    ).length;
    this.kpis.avg = this.avgResolutionDays(rows);
    this.renderCharts();
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
      const c1 = new Date(t.createdAt ?? t.created).getTime();
      sum += Math.max(0, (Date.now() - c0) / 86_400_000);
    }
    return Math.round(sum / done.length);
  }

  private renderCharts() {
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

  exportPdf() {
    window.print();
  }

  exportExcel() {
    window.alert(this.i18n.instant('errors.http.notImplemented'));
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
