import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LeaveRequestApiService } from '../../core/api/leave-request-api.service';
import { LeaveRequestDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';

@Component({
  selector: 'app-leave-requests',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    LatinDigitsPipe,
    ErpAutoReferenceFieldComponent,
    SrsDataTableComponent
  ],
  templateUrl: './leave-requests.component.html',
  styleUrl: './leave-requests.component.css'
})
export class LeaveRequestsComponent implements OnInit {
  mine: LeaveRequestDto[] = [];
  adminRows: LeaveRequestDto[] = [];
  adminLoaded = false;

  lastCreatedLeaveId: string | null = null;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: LeaveRequestApiService,
    private readonly i18n: I18nService
  ) {
    this.form = this.fb.group({
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit(): void {
    this.refreshMine();
    this.api.listAllAdmin().subscribe({
      next: (rows) => {
        this.adminRows = rows ?? [];
        this.adminLoaded = true;
      },
      error: () => {
        this.adminRows = [];
        this.adminLoaded = true;
      }
    });
  }

  refreshMine(): void {
    this.api.listMine().subscribe({
      next: (rows) => (this.mine = rows ?? []),
      error: () => (this.mine = [])
    });
  }

  /** Localized status text; falls back to raw code if unknown. */
  statusLabel(code: string | undefined): string {
    const c = (code ?? '').trim().toUpperCase();
    const key = `leave.statusCodes.${c}`;
    const resolved = this.i18n.instant(key);
    return resolved === key ? (code ?? '—') : resolved;
  }

  /** Badge style: pending = neutral/warning tone, approved = in, rejected = out. */
  statusBadgeClass(code: string | undefined): string {
    switch ((code ?? '').toUpperCase()) {
      case 'APPROVED':
        return 'in';
      case 'REJECTED':
        return 'out';
      case 'PENDING':
        return 'pending';
      default:
        return '';
    }
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
        error: () => undefined
      });
  }

  decide(row: LeaveRequestDto, status: 'APPROVED' | 'REJECTED'): void {
    this.api.decide(row.id, { statusCode: status, decisionNote: null }).subscribe({
      next: () => {
        this.refreshMine();
        this.api.listAllAdmin().subscribe((r) => (this.adminRows = r ?? []));
      },
      error: () => undefined
    });
  }
}
