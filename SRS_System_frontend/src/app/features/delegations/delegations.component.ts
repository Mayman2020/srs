import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthorityDelegationApiService } from '../../core/api/authority-delegation-api.service';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { AuthorityDelegationDto, UserListDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { matchesTableSearch } from '../../core/util/table-text-filter';

@Component({
  selector: 'app-delegations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    ErpAutoReferenceFieldComponent
  ],
  templateUrl: './delegations.component.html',
  styleUrl: './delegations.component.css'
})
export class DelegationsComponent implements OnInit {
  rows: AuthorityDelegationDto[] = [];
  users: UserListDto[] = [];
  loading = true;

  /** Client-side filter across table columns. */
  tableSearch = '';

  /** Shown after successful create; cleared when starting a new submission. */
  lastCreatedDelegationId: string | null = null;

  readonly form;

  constructor(
    private readonly api: AuthorityDelegationApiService,
    private readonly usersApi: UserDirectoryApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly dialogService: DialogService
  ) {
    this.form = this.fb.group({
      delegateUserId: ['', Validators.required],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
      allowedCorrespondenceTypeCodes: [''],
      allowedConfidentialityCodes: [''],
      canSignOnBehalf: [false],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.refresh();
    this.usersApi.list(0, 200).subscribe({
      next: (p) => (this.users = p.content ?? []),
      error: () => (this.users = [])
    });
  }

  refresh(): void {
    this.loading = true;
    this.api.list().subscribe({
      next: (list) => {
        this.rows = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.lastCreatedDelegationId = null;
    const v = this.form.getRawValue();
    this.api
      .create({
        delegateUserId: v.delegateUserId!,
        validFrom: v.validFrom!,
        validTo: v.validTo!,
        allowedCorrespondenceTypeCodes: v.allowedCorrespondenceTypeCodes?.trim() || null,
        allowedConfidentialityCodes: v.allowedConfidentialityCodes?.trim() || null,
        canSignOnBehalf: !!v.canSignOnBehalf,
        notes: v.notes?.trim() || null
      })
      .subscribe({
        next: (created) => {
          this.lastCreatedDelegationId = created?.id ?? null;
          this.form.reset({ canSignOnBehalf: false });
          this.refresh();
        },
        error: () => undefined
      });
  }

  confirmRevoke(row: AuthorityDelegationDto): void {
    this.dialogService
      .openConfirm({
        titleKey: 'delegations.revokeConfirmTitle',
        messageKey: 'delegations.revokeConfirmMessage',
        confirmButton: {
          labelKey: 'delegations.revokeConfirm',
          color: 'warn'
        },
        cancelButton: {
          labelKey: 'common.close'
        }
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.revoke(row.id).subscribe({
          next: () => {
            this.refresh();
          },
          error: () => undefined
        });
      });
  }

  userLabel(u: UserListDto): string {
    const name = this.i18n.currentLang() === 'en' ? u.fullNameEn : u.fullNameAr;
    return `${name} (${u.username})`;
  }

  delegatorName(d: AuthorityDelegationDto): string {
    return this.i18n.currentLang() === 'en' ? d.delegator.fullNameEn : d.delegator.fullNameAr;
  }

  delegateName(d: AuthorityDelegationDto): string {
    return this.i18n.currentLang() === 'en' ? d.delegate.fullNameEn : d.delegate.fullNameAr;
  }

  filteredRows(): AuthorityDelegationDto[] {
    const q = this.tableSearch;
    const open = this.i18n.instant('common.open');
    return this.rows.filter((d) =>
      matchesTableSearch(q, [
        this.delegatorName(d),
        this.delegateName(d),
        d.delegator.username,
        d.delegate.username,
        d.delegator.fullNameAr,
        d.delegator.fullNameEn,
        d.delegate.fullNameAr,
        d.delegate.fullNameEn,
        d.validFrom,
        d.validTo,
        d.notes,
        d.canSignOnBehalf,
        open
      ])
    );
  }
}
