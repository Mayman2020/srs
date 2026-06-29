import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { DepartmentFlatDto } from '../../core/api/api-types';
import { OrgRoutingApiService, RoutingChain } from '../../core/api/org-routing-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';

export interface WorkflowForwardDialogData {
  dialogTitle?: string;
  routingFromDepartmentId?: number | null;
}

export interface WorkflowForwardDialogResult {
  targetDepartmentId: number;
}

@Component({
  selector: 'app-workflow-forward-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, TranslatePipe, ErpDialogComponent],
  template: `
    <app-erp-dialog
      [titleKey]="data.dialogTitle ? '' : 'transactionDetails.workflowForwardDeptPrompt'"
      [titleText]="data.dialogTitle"
      icon="forward"
    >
      <label class="field">
        <span>{{ 'transactionDetails.workflowForwardDeptPrompt' | t }}</span>
        <select class="custom-input" [(ngModel)]="fromDepartmentId" (ngModelChange)="refreshPreview()">
          <option *ngFor="let d of departments" [ngValue]="d.id">{{ deptLabel(d) }}</option>
        </select>
      </label>
      <label class="field">
        <span>{{ 'workflowQuickAction.targetDepartment' | t }}</span>
        <select class="custom-input" [(ngModel)]="targetDepartmentId" (ngModelChange)="refreshPreview()">
          <option [ngValue]="null">{{ 'common.select' | t }}</option>
          <option *ngFor="let d of departments" [ngValue]="d.id">{{ deptLabel(d) }}</option>
        </select>
      </label>
      <div class="routing-mini" *ngIf="routingPreviewLoading">{{ 'common.loading' | t }}</div>
      <div class="routing-mini" *ngIf="routingChain && !routingPreviewLoading">
        <span class="routing-mini__label">{{ 'createTx.routingPreview.title' | t }}</span>
        <div class="routing-mini__chain">
          <span>{{ routingStopLabel(routingChain.originator) }}</span>
          <span *ngFor="let stop of routingChain.stops"> → {{ routingStopLabel(stop) }}</span>
          <span> → {{ routingStopLabel(routingChain.target) }}</span>
        </div>
      </div>
      <div erpDialogActions>
        <button mat-button type="button" (click)="cancel()">{{ 'common.close' | t }}</button>
        <button mat-flat-button type="button" color="primary" (click)="confirm()" [disabled]="!targetDepartmentId">
          {{ 'common.apply' | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        margin-bottom: 0.85rem;
        font-size: 0.9rem;
      }
      .custom-input {
        padding: 0.45rem 0.6rem;
        border-radius: 6px;
        border: 1px solid var(--border, #d1d5db);
        font: inherit;
      }
      .routing-mini {
        margin-top: 0.5rem;
        padding: 0.65rem 0.75rem;
        border-radius: 8px;
        background: var(--surface-muted, #f3f4f6);
        font-size: 0.85rem;
      }
      .routing-mini__label {
        display: block;
        font-weight: 600;
        margin-bottom: 0.25rem;
      }
      .routing-mini__chain {
        line-height: 1.45;
      }
    `
  ]
})
export class WorkflowForwardDialogComponent implements OnInit {
  private readonly deptApi = inject(DepartmentApiService);
  private readonly routingApi = inject(OrgRoutingApiService);
  private readonly i18n = inject(I18nService);

  departments: DepartmentFlatDto[] = [];
  fromDepartmentId: number | null = null;
  targetDepartmentId: number | null = null;
  routingChain: RoutingChain | null = null;
  routingPreviewLoading = false;

  constructor(
    private readonly dialogRef: MatDialogRef<WorkflowForwardDialogComponent, WorkflowForwardDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: WorkflowForwardDialogData
  ) {}

  ngOnInit(): void {
    this.fromDepartmentId = this.data.routingFromDepartmentId ?? null;
    this.deptApi.list().subscribe({
      next: (rows) => {
        this.departments = rows ?? [];
        if (!this.fromDepartmentId && this.departments.length) {
          this.fromDepartmentId = this.departments[0].id;
        }
      }
    });
  }

  deptLabel(d: DepartmentFlatDto): string {
    return this.i18n.currentLang() === 'en' ? d.nameEn : d.nameAr;
  }

  routingStopLabel(stop: { departmentNameAr: string; departmentNameEn: string }): string {
    return this.i18n.currentLang() === 'ar' ? stop.departmentNameAr : stop.departmentNameEn;
  }

  refreshPreview(): void {
    const fromId = Number(this.fromDepartmentId);
    const toId = Number(this.targetDepartmentId);
    if (!Number.isFinite(fromId) || !Number.isFinite(toId) || fromId === toId) {
      this.routingChain = null;
      return;
    }
    this.routingPreviewLoading = true;
    this.routingApi.preview(fromId, toId).subscribe({
      next: (chain) => {
        this.routingChain = chain;
        this.routingPreviewLoading = false;
      },
      error: () => {
        this.routingChain = null;
        this.routingPreviewLoading = false;
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (!this.targetDepartmentId) {
      return;
    }
    this.dialogRef.close({ targetDepartmentId: this.targetDepartmentId });
  }
}
