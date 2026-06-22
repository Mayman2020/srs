import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { LetterTemplateApiService } from '../../core/api/letter-template-api.service';
import { LetterTemplateAdminDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-letter-template-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './letter-template-admin.component.html',
  styleUrl: './letter-template-admin.component.scss'
})
export class LetterTemplateAdminComponent implements OnInit {
  rows: LetterTemplateAdminDto[] = [];
  loading = true;
  editing: LetterTemplateAdminDto | null = null;
  editorOpen = false;
  readonly form;

  constructor(
    private readonly api: LetterTemplateApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly dialog: DialogService,
    private readonly toast: NotificationService
  ) {
    this.form = this.fb.group({
      code: ['', Validators.required],
      nameAr: ['', Validators.required],
      nameEn: ['', Validators.required],
      bodyHtml: [''],
      templateFilePath: [''],
      sortOrder: [0],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.listAdmin().subscribe({
      next: (rows) => {
        this.rows = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
        this.toast.error('letterTemplates.loadFailed');
      }
    });
  }

  label(row: LetterTemplateAdminDto): string {
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  startCreate(): void {
    this.editing = null;
    this.editorOpen = true;
    this.form.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      bodyHtml: '',
      templateFilePath: '',
      sortOrder: 0,
      active: true
    });
    this.form.get('code')?.enable();
  }

  startEdit(row: LetterTemplateAdminDto): void {
    this.editing = row;
    this.editorOpen = true;
    this.form.reset({
      code: row.code,
      nameAr: row.nameAr,
      nameEn: row.nameEn,
      bodyHtml: row.bodyHtml ?? '',
      templateFilePath: row.templateFilePath ?? '',
      sortOrder: row.sortOrder ?? 0,
      active: row.active
    });
    this.form.get('code')?.disable();
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    const payload = {
      nameAr: String(v.nameAr).trim(),
      nameEn: String(v.nameEn).trim(),
      bodyHtml: String(v.bodyHtml ?? ''),
      templateFilePath: String(v.templateFilePath ?? '').trim() || null,
      sortOrder: Number(v.sortOrder ?? 0),
      active: !!v.active
    };
    if (this.editing) {
      this.api.update(this.editing.id, payload).subscribe({
        next: () => {
          this.editing = null;
          this.reload();
          this.toast.success('letterTemplates.saved');
        },
        error: () => this.toast.error('letterTemplates.saveFailed')
      });
      return;
    }
    this.api
      .create({ ...payload, code: String(v.code).trim() })
      .subscribe({
        next: () => {
          this.reload();
          this.toast.success('letterTemplates.saved');
        },
        error: () => this.toast.error('letterTemplates.saveFailed')
      });
  }

  remove(row: LetterTemplateAdminDto): void {
    this.dialog
      .openConfirm({
        titleKey: 'letterTemplates.deleteTitle',
        messageKey: 'letterTemplates.deleteConfirm',
        confirmButton: { labelKey: 'common.delete', color: 'warn' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.delete(row.id).subscribe({
          next: () => {
            this.reload();
            this.toast.success('letterTemplates.deleted');
          },
          error: () => this.toast.error('letterTemplates.saveFailed')
        });
      });
  }
}
