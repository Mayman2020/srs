import {
  Component,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnDestroy
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Chart, registerables } from 'chart.js';
import { TopbarComponent } from '../../layout/topbar/topbar.component';

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reports.html',
  styleUrls: ['./reports.css']
})
export class ReportsComponent implements AfterViewInit, OnDestroy {

  reportForm: FormGroup;

  @ViewChild('trendCanvas', { static: false }) trendCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusCanvas', { static: false }) statusCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deptBarCanvas', { static: false }) deptBarCanvas!: ElementRef<HTMLCanvasElement>;



  trendChart!: Chart;
  statusChart!: Chart;
  deptBarChart!: Chart;


  kpis = {
    total: 0,
    done: 0,
    late: 0,
    avg: 0
  };

  constructor(private fb: FormBuilder) {
    this.reportForm = this.fb.group({
      from: [''],
      to: [''],
      type: [''],
      status: ['']
    });
  }

  ngAfterViewInit(): void {
         this.initCharts();

  }



  ngOnDestroy(): void {
    this.trendChart?.destroy();
    this.statusChart?.destroy();
    this.deptBarChart?.destroy(); // لازم تضيف دي
  }

  initCharts() {

    this.trendChart = new Chart(this.trendCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: ['س1', 'س2', 'س3', 'س4', 'س5', 'س6', 'س7'],
        datasets: [{
          label: 'حركة المعاملات',
          data: [45, 52, 41, 66, 58, 72, 63],
          borderColor: '#0f6b4d',
          backgroundColor: 'rgba(15,107,77,0.15)',
          tension: 0.4,
          fill: true,
          pointRadius: 5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });

    this.statusChart = new Chart(this.statusCanvas.nativeElement, {
      type: 'doughnut',
      data: {
        labels: ['جديدة', 'قيد الإجراء', 'معادة', 'منجزة'],
        datasets: [{
          data: [20, 45, 10, 25],
          backgroundColor: [
            '#1b7f5e',
            '#0f6b4d',
            '#f59e0b',
            '#22c55e'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });

    this.deptBarChart = new Chart(this.deptBarCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: [
          'الاتصالات الإدارية',
          'الموارد البشرية',
          'الشؤون القانونية',
          'المالية',
          'المشتريات'
        ],
        datasets: [
          {
            label: 'منجزة',
            data: [128, 95, 74, 87, 61],
            backgroundColor: '#9ad3b3'
          },
          {
            label: 'متأخرة',
            data: [12, 18, 6, 9, 14],
            backgroundColor: '#f4c27a'
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

  runReport() {
    const total = Math.floor(Math.random() * 300) + 200;
    const done = Math.floor(total * 0.65);
    const late = Math.floor(total * 0.12);
    const avg = Math.floor(Math.random() * 8) + 3;

    this.kpis = { total, done, late, avg };
  }

  exportPdf() {
    window.print();
  }

  exportExcel() {
    alert('Excel Export Demo');
  }
}
