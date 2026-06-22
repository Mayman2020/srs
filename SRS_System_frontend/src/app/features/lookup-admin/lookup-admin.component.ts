import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LookupTableAdminApiService, LookupUpsertBody } from '../../core/api/lookup-table-admin-api.service';
import { LookupCatalogDto, LookupRowAdminDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { DialogService } from '../../core/services/dialog.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { matchesTableSearch } from '../../core/util/table-text-filter';

@Component({
  selector: 'app-lookup-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslatePipe, ErpAutoReferenceFieldComponent],
  templateUrl: './lookup-admin.component.html',
  styleUrl: './lookup-admin.component.css'
})
export class LookupAdminComponent implements OnInit {
  catalog: LookupCatalogDto[] = [];
  selectedCode = '';
  rows: LookupRowAdminDto[] = [];
  loadingCatalog = true;
  loadingRows = false;
  parentRows: LookupRowAdminDto[] = [];
  tableSearch = '';

  readonly form;

  editing: LookupRowAdminDto | null = null;

  constructor(
    private readonly api: LookupTableAdminApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly lookupLabels: LookupLabelsService,
    private readonly dialogService: DialogService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      code: ['', Validators.required],
      nameAr: ['', Validators.required],
      nameEn: ['', Validators.required],
      description: [''],
      sortOrder: [0],
      active: [true],
      parentId: [null as number | null],
      terminal: [false],
      slaDays: [null as number | null],
      restrictsExport: [false],
      requiresClearance: [false],
      uiVariant: ['neutral' as string],
      initial: [false]
    });
  }

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: this.api.catalog(),
      setLoading: (loading) => (this.loadingCatalog = loading),
      next: (c) => {
        this.catalog = c ?? [];
        if (this.catalog.length && !this.selectedCode) {
          this.selectLookup(this.catalog[0].lookupCode);
        }
      },
      error: () => {
        this.catalog = [];
      }
    });
  }

  catalogLabel(c: LookupCatalogDto): string {
    return this.i18n.currentLang() === 'en' ? c.nameEn : c.nameAr;
  }

  selectLookup(lookupCode: string): void {
    this.selectedCode = lookupCode;
    this.editing = null;
    this.form.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      description: '',
      sortOrder: 0,
      active: true,
      parentId: null,
      terminal: false,
      slaDays: null,
      restrictsExport: false,
      requiresClearance: false,
      uiVariant: 'neutral',
      initial: false
    });
    this.loadRows();
    this.loadParentOptions();
  }

  private loadRows(): void {
    if (!this.selectedCode) return;
    subscribePageLoad({
      cdr: this.cdr,
      source: this.api.listRows(this.selectedCode),
      setLoading: (loading) => (this.loadingRows = loading),
      next: (r) => {
        this.rows = r ?? [];
      },
      error: () => {
        this.rows = [];
      }
    });
  }

  filteredRows(): LookupRowAdminDto[] {
    return this.rows.filter((row) =>
      matchesTableSearch(this.tableSearch, [
        row.id,
        row.code,
        row.nameAr,
        row.nameEn,
        row.description,
        row.sortOrder,
        row.active,
        row.parentId,
        row.terminal,
        row.slaDays,
        row.uiVariant
      ])
    );
  }

  /** Parent dropdown: correspondence_status → correspondence_type rows; classification → other classifications. */
  private loadParentOptions(): void {
    this.parentRows = [];
    if (this.selectedCode === 'correspondence_status') {
      this.api.listRows('correspondence_type').subscribe({
        next: (r) => (this.parentRows = r ?? []),
        error: () => (this.parentRows = [])
      });
      return;
    }
    if (this.selectedCode === 'classification') {
      this.api.listRows('classification').subscribe({
        next: (r) => (this.parentRows = r ?? []),
        error: () => (this.parentRows = [])
      });
    }
  }

  startCreate(): void {
    this.editing = null;
    this.form.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      description: '',
      sortOrder: 0,
      active: true,
      parentId: null,
      terminal: false,
      slaDays: null,
      restrictsExport: false,
      requiresClearance: false,
      uiVariant: 'neutral',
      initial: false
    });
  }

  startEdit(row: LookupRowAdminDto): void {
    this.editing = row;
    this.form.patchValue({
      code: row.code,
      nameAr: row.nameAr,
      nameEn: row.nameEn,
      description: row.description ?? '',
      sortOrder: row.sortOrder,
      active: row.active,
      parentId: row.parentId,
      terminal: !!row.terminal,
      slaDays: row.slaDays,
      restrictsExport: !!row.restrictsExport,
      requiresClearance: !!row.requiresClearance,
      uiVariant: row.uiVariant ?? 'neutral',
      initial: !!row.initial
    });
  }

  cancelEdit(): void {
    this.editing = null;
    this.startCreate();
  }

  submit(): void {
    if (this.form.invalid || !this.selectedCode) return;
    const v = this.form.getRawValue();
    const code = (v.code ?? '').trim();
    const nameAr = (v.nameAr ?? '').trim();
    const nameEn = (v.nameEn ?? '').trim();
    if (!code || !nameAr || !nameEn) return;
    const body: LookupUpsertBody = {
      code,
      nameAr,
      nameEn,
      description: v.description?.trim() || null,
      sortOrder: Number(v.sortOrder) || 0,
      active: !!v.active,
      parentId: v.parentId != null ? Number(v.parentId) : null,
      terminal:
        this.selectedCode === 'correspondence_status' || this.selectedCode === 'leave_status'
          ? !!v.terminal
          : null,
      slaDays: this.selectedCode === 'priority' ? (v.slaDays != null ? Number(v.slaDays) : null) : null,
      restrictsExport:
        this.selectedCode === 'confidentiality' ? !!v.restrictsExport : null,
      requiresClearance:
        this.selectedCode === 'confidentiality' ? !!v.requiresClearance : null,
      uiVariant:
        this.selectedCode === 'correspondence_status' ||
        this.selectedCode === 'priority' ||
        this.selectedCode === 'leave_status'
          ? (v.uiVariant ?? 'neutral')
          : null,
      initial: this.selectedCode === 'leave_status' ? !!v.initial : null
    };
    const req = this.editing
      ? this.api.update(this.selectedCode, this.editing.id, body)
      : this.api.create(this.selectedCode, body);
    req.subscribe({
      next: () => {
        this.lookupLabels.load().subscribe({ error: () => undefined });
        this.loadRows();
        this.loadParentOptions();
        this.startCreate();
      },
      error: () => undefined
    });
  }

  confirmDelete(row: LookupRowAdminDto): void {
    this.dialogService
      .openConfirm({
        titleKey: 'lookupAdmin.deleteTitle',
        messageKey: 'lookupAdmin.deleteMessage',
        confirmButton: {
          labelKey: 'lookupAdmin.deleteConfirm',
          color: 'warn'
        },
        cancelButton: {
          labelKey: 'common.close'
        }
      })
      .subscribe((ok) => {
        if (!ok || !this.selectedCode) return;
        this.api.delete(this.selectedCode, row.id).subscribe({
          next: () => {
            this.lookupLabels.load().subscribe({ error: () => undefined });
            this.loadRows();
            this.loadParentOptions();
          },
          error: () => undefined
        });
      });
  }

  showParentColumn(): boolean {
    return this.selectedCode === 'correspondence_status' || this.selectedCode === 'classification';
  }

  showTerminal(): boolean {
    return this.selectedCode === 'correspondence_status' || this.selectedCode === 'leave_status';
  }

  showInitial(): boolean {
    return this.selectedCode === 'leave_status';
  }

  showStatusUiVariant(): boolean {
    return (
      this.selectedCode === 'correspondence_status' ||
      this.selectedCode === 'priority' ||
      this.selectedCode === 'leave_status'
    );
  }

  statusUiVariantOptions(): readonly string[] {
    return ['success', 'danger', 'warning', 'info', 'secondary', 'neutral'];
  }

  showSla(): boolean {
    return this.selectedCode === 'priority';
  }

  showConfFlags(): boolean {
    return this.selectedCode === 'confidentiality';
  }

  parentLabel(row: LookupRowAdminDto): string {
    return `${row.code} — ${this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr}`;
  }

  filteredParents(): LookupRowAdminDto[] {
    if (this.selectedCode !== 'classification' || !this.editing) {
      return this.parentRows;
    }
    return this.parentRows.filter((p) => p.id !== this.editing!.id);
  }
}
