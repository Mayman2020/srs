import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminConsoleApiService } from '../../core/api/admin-console-api.service';
import { LookupTableAdminApiService } from '../../core/api/lookup-table-admin-api.service';
import { LookupRowAdminDto, ServiceWorkflowRouteDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';

@Component({
  selector: 'app-workflow-routes-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, SrsDataTableComponent, ErpPageShellComponent],
  templateUrl: './workflow-routes-admin.component.html',
  styleUrl: './workflow-routes-admin.component.scss'
})
export class WorkflowRoutesAdminComponent implements OnInit {
  private readonly adminApi = inject(AdminConsoleApiService);
  private readonly lookupApi = inject(LookupTableAdminApiService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);
  private readonly toast = inject(NotificationService);

  rows: ServiceWorkflowRouteDto[] = [];
  correspondenceTypes: LookupRowAdminDto[] = [];
  loading = false;
  modalOpen = false;
  editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    correspondenceTypeId: [0, Validators.required],
    processDefinitionKey: ['', Validators.required],
    nameAr: ['', Validators.required],
    nameEn: ['', Validators.required],
    defaultRoute: [false],
    sortOrder: [10, [Validators.required, Validators.min(0)]],
    active: [true]
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    forkJoin({
      routes: this.adminApi.listWorkflowRoutes(),
      types: this.lookupApi.listRows('correspondence_type')
    }).subscribe({
      next: ({ routes, types }) => {
        this.rows = routes ?? [];
        this.correspondenceTypes = types ?? [];
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.correspondenceTypes = [];
        this.loading = false;
        this.toast.error('workflowRoutes.loadFailed');
      }
    });
  }

  typeLabel(typeId: number): string {
    const t = this.correspondenceTypes.find((x) => x.id === typeId);
    if (!t) {
      return String(typeId);
    }
    return this.i18n.currentLang() === 'ar' ? t.nameAr : t.nameEn;
  }

  routeName(row: ServiceWorkflowRouteDto): string {
    return this.i18n.currentLang() === 'ar' ? row.nameAr : row.nameEn;
  }

  openCreate(): void {
    this.editingId = null;
    const firstType = this.correspondenceTypes[0]?.id ?? 0;
    this.form.reset({
      correspondenceTypeId: firstType,
      processDefinitionKey: '',
      nameAr: '',
      nameEn: '',
      defaultRoute: false,
      sortOrder: 10,
      active: true
    });
    this.modalOpen = true;
  }

  openEdit(row: ServiceWorkflowRouteDto): void {
    this.editingId = row.id;
    this.form.reset({
      correspondenceTypeId: row.correspondenceTypeId,
      processDefinitionKey: row.processDefinitionKey,
      nameAr: row.nameAr,
      nameEn: row.nameEn,
      defaultRoute: row.defaultRoute,
      sortOrder: row.sortOrder,
      active: row.active
    });
    this.modalOpen = true;
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    const body = this.form.getRawValue();
    const req = this.editingId
      ? this.adminApi.updateWorkflowRoute(this.editingId, body)
      : this.adminApi.createWorkflowRoute(body);
    req.subscribe({
      next: () => {
        this.modalOpen = false;
        this.reload();
        this.toast.success('workflowRoutes.saved');
      },
      error: () => this.toast.error('workflowRoutes.saveFailed')
    });
  }

  remove(row: ServiceWorkflowRouteDto): void {
    if (!confirm(this.i18n.instant('workflowRoutes.confirmDelete'))) {
      return;
    }
    this.adminApi.deleteWorkflowRoute(row.id).subscribe({
      next: () => {
        this.reload();
        this.toast.success('workflowRoutes.deleted');
      },
      error: () => this.toast.error('workflowRoutes.deleteFailed')
    });
  }

  closeModal(): void {
    this.modalOpen = false;
  }
}
