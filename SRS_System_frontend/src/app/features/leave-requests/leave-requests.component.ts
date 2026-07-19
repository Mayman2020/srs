import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LeaveRequestApiService } from '../../core/api/leave-request-api.service';
import { LeaveRequestDto, LookupItemDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { DateFieldComponent } from '../../shared/components/date-field/date-field.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { matchesTableSearch } from '../../core/util/table-text-filter';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { correspondenceStatusBadgeClass } from '../../core/util/correspondence-status-ui';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-leave-requests',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    LatinDigitsPipe,
    SrsDatePipe,
    ErpAutoReferenceFieldComponent,
    DateFieldComponent,
    SrsDataTableComponent
  ],
  templateUrl: './leave-requests.component.html',
  styleUrl: './leave-requests.component.css'
})
export class LeaveRequestsComponent implements OnInit {
  mine: LeaveRequestDto[] = [];
  adminRows: LeaveRequestDto[] = [];
  adminLoaded = false;
  adminSearchQuery = '';
  leaveStatuses: LookupItemDto[] = [];

  lastCreatedLeaveId: string | null = null;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: LeaveRequestApiService,
    public readonly lookupLabels: LookupLabelsService,
    private readonly i18n: I18nService,
    private readonly toast: NotificationService
  ) {
    this.form = this.fb.group({
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit(): void {
    this.lookupLabels.loadTable(LookupCode.LeaveStatus).subscribe({
      next: (rows) => (this.leaveStatuses = rows ?? []),
      error: () => {
        this.leaveStatuses = [];
        this.toast.error('errors.generic');
      }
    });
    this.refreshMine();
    this.api.listAllAdmin().subscribe({
      next: (rows) => {
        this.adminRows = rows ?? [];
        this.adminLoaded = true;
      },
      error: () => {
        this.adminRows = [];
        this.adminLoaded = true;
        this.toast.error('errors.generic');
      }
    });
  }

  refreshMine(): void {
    this.api.listMine().subscribe({
      next: (rows) => (this.mine = rows ?? []),
      error: () => {
        this.mine = [];
        this.toast.error('errors.generic');
      }
    });
  }

  statusLabel(code: string | undefined): string {
    return this.lookupLabels.label(LookupCode.LeaveStatus, code);
  }

  statusBadgeClass(row: LeaveRequestDto): string {
    const variant =
      row.statusUiVariant ??
      this.leaveStatuses.find((s) => s.code === row.statusCode)?.uiVariant ??
      'neutral';
    return correspondenceStatusBadgeClass(variant);
  }

  initialStatusCode(): string | null {
    return this.leaveStatuses.find((s) => s.dashboardInboundHighlight)?.code ?? null;
  }

  decisionStatuses(): LookupItemDto[] {
    return this.leaveStatuses.filter((s) => s.terminal);
  }

  decisionButtonClass(status: LookupItemDto): string {
    const variant = (status.uiVariant ?? '').toLowerCase();
    if (variant === 'success') return 'edit';
    if (variant === 'danger') return 'delete';
    return '';
  }

  decisionIconClass(status: LookupItemDto): string {
    const variant = (status.uiVariant ?? '').toLowerCase();
    if (variant === 'success') return 'fa-check';
    if (variant === 'danger') return 'fa-xmark';
    return 'fa-circle';
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.lastCreatedLeaveId = null;
    const v = this.form.getRawValue();
    this.api
      .create({
        startDate: v.startDate!,
        endDate: v.endDate!,
        reason: v.reason?.trim() || null
      })
      .subscribe({
        next: (created) => {
          this.lastCreatedLeaveId = created?.id ?? null;
          this.form.reset();
          this.refreshMine();
        },
        error: () => this.toast.error('errors.generic')
      });
  }

  decide(row: LeaveRequestDto, statusCode: string): void {
    this.api.decide(row.id, { statusCode, decisionNote: null }).subscribe({
      next: () => {
        this.refreshMine();
        this.api.listAllAdmin().subscribe((r) => (this.adminRows = r ?? []));
      },
      error: () => this.toast.error('errors.generic')
    });
  }

  filteredAdminRows(): LeaveRequestDto[] {
    return this.adminRows.filter((row) =>
      matchesTableSearch(this.adminSearchQuery, [
        row.username,
        row.fullNameAr,
        row.fullNameEn,
        row.startDate,
        row.endDate,
        row.reason,
        row.statusCode,
        this.statusLabel(row.statusCode)
      ])
    );
  }
}
