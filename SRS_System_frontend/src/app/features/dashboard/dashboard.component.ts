import {
  afterNextRender,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { MatIcon } from '@angular/material/icon';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { DashboardBucketDto, DashboardResponseDto } from '../../core/api/api-types';
import { TransactionService } from '../../core/services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { ThemeService } from '../../core/services/theme.service';
import { chartColorForUiVariant, chartThemeColors } from '../../core/util/chart-ui-variant-colors';

export type DashboardRecentRow = {
  id: string;
  referenceNumber: string;
  typeCode: string;
  subject: string;
  created: string;
  statusCode: string;
  statusUiVariant: string | null;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatIcon,
    TranslatePipe,
    LookupTranslatePipe
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  @ViewChild('statusDonut') statusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deptBar') deptBar!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiStatusDonut') aiStatusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiPriorityBar') aiPriorityBar!: ElementRef<HTMLCanvasElement>;

  /** SLA ring animation — signal updates schedule CD without `detectChanges()`. */
  readonly animatedSlaPercent = signal(0);

  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);
  private readonly theme = inject(ThemeService);
  private readonly formatUi = inject(UiFormatService);
  private statusDonutChart?: Chart;
  private deptBarChart?: Chart;
  private aiStatusDonutChart?: Chart;
  private aiPriorityBarChart?: Chart;

  constructor(
    public router: Router,
    private dashboardApi: DashboardApiService,
    private lookupLabels: LookupLabelsService,
    private transactionService: TransactionService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.theme.mode$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.dash) {
          this.renderCharts();
        }
      });
  }

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

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.recentLoading = v),
      source: forkJoin({
        dash: this.dashboardApi.getDashboard(),
        recent: this.transactionService.listPage({ page: 0, size: 50 }),
        correspondenceTypes: this.lookupLabels
          .loadTable(LookupCode.CorrespondenceType)
          .pipe(catchError(() => of([]))),
        correspondenceStatuses: this.lookupLabels
          .loadTable(LookupCode.CorrespondenceStatus)
          .pipe(catchError(() => of([])))
      }),
      next: ({ dash, recent }) => {
        this.dash = dash;
        this.total = dash.totalCorrespondences;
        this.overdueCount = dash.overdueCount;
        this.done = dash.kpiSlaDoneCount ?? 0;
        this.inProgress = dash.kpiPipelineCount ?? 0;
        this.incoming = dash.kpiInboxCount ?? 0;
        this.outbound = dash.kpiOutboundCount ?? 0;

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
          statusCode: t.statusCode,
          statusUiVariant: t.statusUiVariant ?? null,
        }));
        this.recentRows = this.recentAll.slice(0, 8);
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
        this.dash = null;
        this.overdueCount = 0;
        this.slaOnTime = 0;
        this.slaLate = 0;
        this.slaAction = 0;
      }
    });
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

  private bucketSeries(buckets: DashboardBucketDto[]): {
    labels: string[];
    data: number[];
    colors: string[];
  } {
    const lang = this.i18n.currentLang();
    const theme = chartThemeColors();
    const sorted = [...(buckets ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
    return {
      labels: sorted.map((b) => (lang === 'en' ? b.nameEn : b.nameAr)),
      data: sorted.map((b) => b.count),
      colors: sorted.map((b) => chartColorForUiVariant(b.uiVariant, theme))
    };
  }

  private renderCharts() {
    if (!this.dash) {
      return;
    }

    const colors = chartThemeColors();
    const status = this.bucketSeries(this.dash.byStatus);
    const priorities = this.bucketSeries(this.dash.byPriority);

    const countAxisLabel = this.i18n.instant('dashboard.chart.count');

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
            color: colors.text,
            usePointStyle: true,
            pointStyle: 'circle' as const
          }
        }
      }
    };

    if (this.statusDonut?.nativeElement) {
      this.statusDonutChart?.destroy();
      this.statusDonutChart = new Chart(this.statusDonut.nativeElement, {
        type: 'doughnut',
        data: {
          labels: status.labels,
          datasets: [
            {
              data: status.data,
              backgroundColor: status.colors,
              borderColor: colors.surface,
              borderWidth: 3,
              hoverOffset: 10
            }
          ]
        },
        options: doughnutOpts
      });
    }

    if (this.deptBar?.nativeElement) {
      this.deptBarChart?.destroy();
      this.deptBarChart = new Chart(this.deptBar.nativeElement, {
        type: 'bar',
        data: {
          labels: priorities.labels,
          datasets: [
            {
              label: countAxisLabel,
              data: priorities.data,
              backgroundColor: priorities.colors,
              borderColor: colors.surface,
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
            y: {
              beginAtZero: true,
              ticks: { color: colors.text },
              grid: { color: colors.grid }
            },
            x: {
              ticks: { color: colors.text },
              grid: { display: false }
            }
          }
        }
      });
    }

    if (this.aiStatusDonut?.nativeElement) {
      this.aiStatusDonutChart?.destroy();
      this.aiStatusDonutChart = new Chart(this.aiStatusDonut.nativeElement, {
        type: 'doughnut',
        data: {
          labels: status.labels,
          datasets: [
            {
              data: status.data,
              backgroundColor: status.colors,
              borderColor: colors.surface,
              borderWidth: 3
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: { legend: { labels: { color: colors.text } } }
        }
      });
    }

    if (this.aiPriorityBar?.nativeElement) {
      this.aiPriorityBarChart?.destroy();
      this.aiPriorityBarChart = new Chart(this.aiPriorityBar.nativeElement, {
        type: 'bar',
        data: {
          labels: priorities.labels,
          datasets: [
            {
              label: this.i18n.instant('dashboard.chart.priorityAxis'),
              data: priorities.data,
              backgroundColor: priorities.colors,
              borderRadius: 8
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: { legend: { labels: { color: colors.text } } },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { color: colors.text },
              grid: { color: colors.grid }
            },
            x: {
              ticks: { color: colors.text },
              grid: { display: false }
            }
          }
        }
      });
    }
  }

  format(n: number): string {
    return this.formatUi.formatNumber(n);
  }

  private chartColors(): { text: string; grid: string; surface: string } {
    const theme = chartThemeColors();
    return { text: theme.text, grid: theme.grid, surface: theme.surface };
  }

  openTransctions(): void {
    this.router.navigate(['/correspondence']);
  }

  openTransaction(id: string): void {
    this.router.navigate(['/correspondence', id]);
  }

  trackByRecentId(_i: number, row: DashboardRecentRow): string {
    return row.id;
  }

  statusPillClass(variant: string | null): string {
    const value = (variant ?? '').toLowerCase();
    if (value.includes('danger') || value.includes('error') || value.includes('reject') || value.includes('return')) {
      return 'bad';
    }
    if (value.includes('warn') || value.includes('pending') || value.includes('progress')) {
      return 'warn';
    }
    if (value.includes('success') || value.includes('done')) {
      return 'ok';
    }
    return '';
  }
}
