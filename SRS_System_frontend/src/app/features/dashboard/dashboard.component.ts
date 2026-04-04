import { Component, ElementRef, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';

import { CommonModule } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { DashboardBucketDto, DashboardResponseDto } from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatIcon, TranslatePipe, LookupTranslatePipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  @ViewChild('statusDonut') statusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deptBar') deptBar!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiStatusDonut') aiStatusDonut!: ElementRef<HTMLCanvasElement>;
  @ViewChild('aiPriorityBar') aiPriorityBar!: ElementRef<HTMLCanvasElement>;

  constructor(
    public router: Router,
    private cdr: ChangeDetectorRef,
    private dashboardApi: DashboardApiService,
    private transactionService: TransactionService,
    private i18n: I18nService
  ) {}

  slaPercent = 0;
  animatedSlaPercent = 0;

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

  transactions: {
    id: string;
    referenceNumber: string;
    typeCode: string;
    subject: string;
    created: string;
    statusCode: string;
  }[] = [];

  ngOnInit(): void {
    forkJoin({
      dash: this.dashboardApi.getDashboard(),
      recent: this.transactionService.listPage({ page: 0, size: 6 })
    }).subscribe({
      next: ({ dash, recent }) => {
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

        this.transactions = recent.map((t) => ({
          id: t.id,
          referenceNumber: t.referenceNumber ?? t.id,
          typeCode: t.typeCode,
          subject: t.subject,
          created: t.created,
          statusCode: t.statusCode
        }));
        this.cdr.detectChanges();
        setTimeout(() => {
          this.animateSla();
          this.renderCharts();
        }, 0);
      },
      error: () => {
        this.total = 0;
        this.transactions = [];
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
        this.animatedSlaPercent = current;
        this.cdr.detectChanges();
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
}
