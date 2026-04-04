import { Component, ElementRef, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';

import { CommonModule } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { CodeCountDto, DashboardChartsDto } from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';

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
    private i18n: I18nService,
    private lookupLabels: LookupLabelsService
  ) {}

  slaPercent = 0;
  animatedSlaPercent = 0;

  total = 0;
  done = 0;
  inProgress = 0;
  incoming = 0;
  outbound = 0;

  aiRisk = 0;
  aiConfidence = 0;
  aiNextMonth = 0;

  private chartData: DashboardChartsDto | null = null;

  transactions: {
    id: string;
    typeCode: string;
    subject: string;
    created: string;
    statusCode: string;
  }[] = [];

  ngOnInit(): void {
    forkJoin({
      dash: this.dashboardApi.getSummary(),
      charts: this.dashboardApi.getCharts(),
      recent: this.transactionService.listPage(0, 6)
    }).subscribe({
      next: ({ dash, charts, recent }) => {
        this.chartData = charts;
        this.total = dash.totalCorrespondence;
        this.done = dash.completedCount;
        this.inProgress = dash.inProgressCount;
        this.incoming = dash.inboundCount;
        this.outbound = dash.outboundCount;
        const denom = Math.max(this.total, 1);
        this.slaPercent = Math.min(100, Math.round((this.done / denom) * 100));
        this.aiRisk = Math.min(100, this.inProgress * 3);
        this.aiConfidence = Math.min(100, 60 + Math.min(this.done, 20));
        this.aiNextMonth = this.total + Math.round(this.inProgress * 0.5);
        this.transactions = recent.map((t) => ({
          id: t.referenceNumber ?? t.id,
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
        this.chartData = null;
      }
    });
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

  private seriesForLookupTable(
    buckets: CodeCountDto[],
    table: string
  ): { labels: string[]; data: number[] } {
    const countMap = new Map(buckets.map((b) => [b.code, b.count]));
    const rows = this.lookupLabels.orderedRows(table);
    return {
      labels: rows.map((r) => this.lookupLabels.displayName(r)),
      data: rows.map((r) => countMap.get(r.code) ?? 0)
    };
  }

  private renderCharts() {
    if (!this.chartData) {
      return;
    }

    const status = this.seriesForLookupTable(
      this.chartData.byCorrespondenceStatus,
      'correspondenceStatus'
    );
    const types = this.seriesForLookupTable(
      this.chartData.byCorrespondenceType,
      'correspondenceType'
    );
    const priorities = this.seriesForLookupTable(this.chartData.byPriority, 'priority');

    const countAxisLabel = this.i18n.instant('dashboard.chart.count');

    if (this.statusDonut?.nativeElement) {
      new Chart(this.statusDonut.nativeElement, {
        type: 'doughnut',
        data: {
          labels: status.labels,
          datasets: [
            {
              data: status.data,
              backgroundColor: ['#10B981', '#F59E0B', '#0da1eb', '#6366F1', '#EC4899', '#14B8A6'],
              borderColor: '#FFFFFF',
              borderWidth: 3,
              hoverOffset: 10
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: true,
          plugins: {
            legend: {
              position: 'bottom',
              rtl: true,
              labels: {
                font: { size: 13, family: 'Cairo, sans-serif', weight: 'bold' },
                padding: 15,
                usePointStyle: true,
                pointStyle: 'circle'
              }
            }
          }
        }
      });
    }

    if (this.deptBar?.nativeElement) {
      new Chart(this.deptBar.nativeElement, {
        type: 'bar',
        data: {
          labels: types.labels,
          datasets: [
            {
              label: countAxisLabel,
              data: types.data,
              backgroundColor: ['#0B6E4F', '#10B981', '#34D399', '#6EE7B7', '#A7F3D0', '#D1FAE5'],
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
              backgroundColor: ['#0da1eb', '#F59E0B', '#10B981', '#6366F1', '#EC4899'],
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
              backgroundColor: ['#44c7ef', '#F59E0B', '#10B981', '#8B5CF6', '#F43F5E'],
              borderRadius: 8
            }
          ]
        },
        options: { responsive: true, maintainAspectRatio: true }
      });
    }
  }

  format(n: number): string {
    return n.toLocaleString('ar-SA');
  }

  openTransctions(): void {
    this.router.navigate(['/transactions']);
  }
}
