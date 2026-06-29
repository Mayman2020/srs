import { CommonModule } from '@angular/common';
import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OrganizationFlatDto } from '../../core/api/api-types';
import { UpsertOrganizationRequestDto } from '../../core/api/organization-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../../shared/erp/erp-dialog.component';

export interface OrgStructureOrganizationDialogData {
  mode: 'create' | 'edit';
  organization?: OrganizationFlatDto;
  organizations: OrganizationFlatDto[];
}

@Component({
  selector: 'app-org-structure-organization-dialog',
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
  templateUrl: './org-structure-organization-dialog.component.html',
  styleUrl: './org-structure-organization-dialog.component.css'
})
export class OrgStructureOrganizationDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly i18n = inject(I18nService);

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(64)]),
    nameAr: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(250)]),
    nameEn: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(250)]),
    parentId: this.fb.control<number | null>(null),
    external: this.fb.nonNullable.control(false),
    description: this.fb.control<string | null>('')
  });

  readonly availableParents: OrganizationFlatDto[];
  readonly titleKey: string;

  constructor(
    private readonly dialogRef: MatDialogRef<
      OrgStructureOrganizationDialogComponent,
      UpsertOrganizationRequestDto
    >,
    @Inject(MAT_DIALOG_DATA) readonly data: OrgStructureOrganizationDialogData
  ) {
    this.titleKey =
      data.mode === 'create' ? 'orgCrud.createTitle' : 'orgCrud.editTitle';
    const selfId = data.organization?.id;
    this.availableParents = [...(data.organizations ?? [])]
      .filter((o) => o.id !== selfId)
      .sort((a, b) => a.id - b.id);

    const current = data.organization;
    this.form.patchValue({
      code: current?.code ?? '',
      nameAr: current?.nameAr ?? '',
      nameEn: current?.nameEn ?? '',
      parentId: current?.parentId ?? null,
      external: current?.external ?? false,
      description: ''
    });
  }

  parentLabel(item: OrganizationFlatDto): string {
    const name = this.i18n.currentLang() === 'en' ? item.nameEn : item.nameAr;
    return `${name} (${item.code || item.id})`;
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
      external: raw.external,
      description: raw.description?.trim() || null
    });
  }
}
