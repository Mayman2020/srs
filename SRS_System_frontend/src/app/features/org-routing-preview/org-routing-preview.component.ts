import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { OrgRoutingApiService, RoutingChain } from '../../core/api/org-routing-api.service';
import { DepartmentFlatDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';

@Component({
  selector: 'app-org-routing-preview',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, ErpPageShellComponent],
  templateUrl: './org-routing-preview.component.html',
  styleUrl: './org-routing-preview.component.scss'
})
export class OrgRoutingPreviewComponent {
  private readonly deptApi = inject(DepartmentApiService);
  private readonly routingApi = inject(OrgRoutingApiService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);
  private readonly toast = inject(NotificationService);

  departments: DepartmentFlatDto[] = [];
  chain: RoutingChain | null = null;
  loading = false;

  readonly form = this.fb.nonNullable.group({
    fromDepartmentId: [0, Validators.required],
    toDepartmentId: [0, Validators.required]
  });

  constructor() {
    this.deptApi.list().subscribe({
      next: (rows) => {
        this.departments = rows ?? [];
        if (this.departments.length >= 2) {
          this.form.patchValue({
            fromDepartmentId: this.departments[0].id,
            toDepartmentId: this.departments[1].id
          });
        }
      },
      error: () => (this.departments = [])
    });
  }

  deptLabel(d: DepartmentFlatDto): string {
    return this.i18n.currentLang() === 'ar' ? d.nameAr : d.nameEn;
  }

  preview(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    this.loading = true;
    this.chain = null;
    this.routingApi.preview(v.fromDepartmentId, v.toDepartmentId).subscribe({
      next: (c) => {
        this.chain = c;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('routingPreview.loadFailed');
      }
    });
  }
}
