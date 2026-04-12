import { CommonModule } from '@angular/common';
import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { DepartmentFlatDto } from '../../core/api/api-types';
import { DepartmentUpsertRequest } from '../../core/api/department-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../../shared/erp/erp-dialog.component';

export interface OrgStructureDepartmentDialogData {
  mode: 'create' | 'edit';
  department?: DepartmentFlatDto;
  parentId: number | null;
  departments: DepartmentFlatDto[];
  blockedParentIds?: number[];
}

@Component({
  selector: 'app-org-structure-department-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    TranslatePipe,
    ErpDialogComponent
  ],
  templateUrl: './org-structure-department-dialog.component.html',
  styleUrl: './org-structure-department-dialog.component.css'
})
export class OrgStructureDepartmentDialogComponent {
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(64)]),
    nameAr: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(250)]),
    nameEn: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(250)]),
    parentId: this.fb.control<number | null>(null),
    sortOrder: this.fb.nonNullable.control(0, [Validators.min(0)])
  });

  readonly availableParents: DepartmentFlatDto[];
  readonly titleKey: string;

  constructor(
    private readonly i18n: I18nService,
    private readonly dialogRef: MatDialogRef<OrgStructureDepartmentDialogComponent, DepartmentUpsertRequest>,
    @Inject(MAT_DIALOG_DATA) readonly data: OrgStructureDepartmentDialogData
  ) {
    this.titleKey =
      data.mode === 'create'
        ? 'orgStructure.tree.createDialogTitle'
        : 'orgStructure.tree.editDialogTitle';

    const blockedIds = new Set<number>(data.blockedParentIds ?? []);
    this.availableParents = [...(data.departments ?? [])]
      .filter((item) => !blockedIds.has(item.id))
      .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);

    const current = data.department;
    this.form.patchValue({
      code: current?.code ?? '',
      nameAr: current?.nameAr ?? '',
      nameEn: current?.nameEn ?? '',
      parentId: data.mode === 'edit' ? (current?.parentId ?? null) : data.parentId,
      sortOrder: current?.sortOrder ?? 0
    });
  }

  parentLabel(item: DepartmentFlatDto): string {
    const name = this.i18n.currentLang() === 'en' ? item.nameEn : item.nameAr;
    return `${name} (${item.code})`;
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.dialogRef.close({
      code: raw.code.trim(),
      nameAr: raw.nameAr.trim(),
      nameEn: raw.nameEn.trim(),
      parentId: raw.parentId,
      sortOrder: raw.sortOrder
    });
  }
}
