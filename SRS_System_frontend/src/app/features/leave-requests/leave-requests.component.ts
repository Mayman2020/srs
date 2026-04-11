import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LeaveRequestApiService } from '../../core/api/leave-request-api.service';
import { LeaveRequestDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';

@Component({
  selector: 'app-leave-requests',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, ErpAutoReferenceFieldComponent],
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
    private readonly api: LeaveRequestApiService
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
