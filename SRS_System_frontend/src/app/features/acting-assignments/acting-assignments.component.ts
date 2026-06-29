import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActingAssignmentApiService } from '../../core/api/acting-assignment-api.service';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import {
  ActingAssignmentDto,
  ActingAssignmentListDto,
  UserListDto
} from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { CapabilitiesService } from '../../core/auth/capabilities.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { DateFieldComponent } from '../../shared/components/date-field/date-field.component';

type ActingTab = 'absent' | 'acting' | 'upcoming' | 'inactive' | 'audit';

/**
 * Slice 4 — Acting manager coverage: who acts for you while absent, who you act for, upcoming /
 * history, and (with `ACTING_ASSIGNMENT_VIEW`) a read-only audit feed.
 */
@Component({
  selector: 'app-acting-assignments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    ErpAutoReferenceFieldComponent,
    DateFieldComponent
  ],
  templateUrl: './acting-assignments.component.html',
  styleUrl: './acting-assignments.component.css'
})
export class ActingAssignmentsComponent implements OnInit {
  list: ActingAssignmentListDto = {
    asAbsent: [],
    asActing: [],
    upcoming: [],
    inactive: []
  };
  auditRows: ActingAssignmentDto[] = [];
  users: UserListDto[] = [];
  loading = true;
  auditLoading = false;
  auditLoaded = false;
  submitting = false;
  lastCreatedId: string | null = null;

  activeTab: ActingTab = 'absent';

  readonly form;

  constructor(
    private readonly api: ActingAssignmentApiService,
    private readonly usersApi: UserDirectoryApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly dialogService: DialogService,
    private readonly toast: NotificationService,
    private readonly auth: AuthTokenService,
    private readonly cap: CapabilitiesService
  ) {
    this.form = this.fb.group({
      absentUserId: [''],
      actingUserId: ['', Validators.required],
      departmentId: [''],
      includeDepartmentSubtree: [false],
      processDefinitionKey: [''],
      taskDefinitionKey: [''],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
      notes: ['']
    });
  }

  get canAdmin(): boolean {
    return this.cap.can('ACTING_ASSIGNMENT_ADMIN');
  }

  get canCreate(): boolean {
    return this.cap.can('ACTING_ASSIGNMENT_MANAGE_OWN') || this.canAdmin;
  }

  get canViewAudit(): boolean {
    return this.cap.can('ACTING_ASSIGNMENT_VIEW');
  }

  get myUserId(): string | null {
    return this.auth.getUserId()?.trim() || null;
  }

  ngOnInit(): void {
    const absentCtl = this.form.get('absentUserId');
    if (this.canAdmin) {
      absentCtl?.setValidators([Validators.required]);
    } else {
      absentCtl?.clearValidators();
    }
    absentCtl?.updateValueAndValidity();

    this.refresh();
    this.usersApi.list(0, 300).subscribe({
      next: (p) => (this.users = p.content ?? []),
      error: () => (this.users = [])
    });
  }

  refresh(): void {
    this.loading = true;
    this.api.listMine().subscribe({
      next: (list) => {
        this.list = list ?? {
          asAbsent: [],
          asActing: [],
          upcoming: [],
          inactive: []
        };
        this.loading = false;
      },
      error: () => {
        this.list = { asAbsent: [], asActing: [], upcoming: [], inactive: [] };
        this.loading = false;
        this.toast.error('acting.loadFailed');
      }
    });
  }

  setTab(tab: ActingTab): void {
    this.activeTab = tab;
    if (tab === 'audit' && this.canViewAudit && !this.auditLoaded) {
      this.loadAudit();
    }
  }

  loadAudit(): void {
    this.auditLoading = true;
    this.api.listAudit().subscribe({
      next: (rows) => {
        this.auditRows = rows ?? [];
        this.auditLoaded = true;
        this.auditLoading = false;
      },
      error: () => {
        this.auditRows = [];
        this.auditLoading = false;
        this.toast.error('acting.auditLoadFailed');
      }
    });
  }

  currentRows(): ActingAssignmentDto[] {
    switch (this.activeTab) {
      case 'absent':
        return this.list.asAbsent;
      case 'acting':
        return this.list.asActing;
      case 'upcoming':
        return this.list.upcoming;
      case 'inactive':
        return this.list.inactive;
      case 'audit':
        return this.auditRows;
      default:
        return [];
    }
  }

  scopeSummary(r: ActingAssignmentDto): string {
    const parts: string[] = [];
    if (r.departmentId != null) {
      parts.push(`${this.i18n.instant('acting.colDept')}: ${r.departmentId}`);
    }
    if (r.includeDepartmentSubtree) {
      parts.push(this.i18n.instant('acting.subtree'));
    }
    if (r.orgLevelCode) {
      parts.push(`${this.i18n.instant('acting.colOrgLevel')}: ${r.orgLevelCode}`);
    }
    if (r.processDefinitionKey) {
      parts.push(r.processDefinitionKey);
    }
    if (r.taskDefinitionKey) {
      parts.push(r.taskDefinitionKey);
    }
    return parts.length ? parts.join(' · ') : this.i18n.instant('acting.scopeAll');
  }

  lifecycleLabel(r: ActingAssignmentDto): string {
    const k = `acting.lifecycle.${r.lifecycleStatus}`;
    return this.i18n.instant(k);
  }

  canRevoke(r: ActingAssignmentDto): boolean {
    if (r.revokedAt) {
      return false;
    }
    const uid = this.myUserId;
    return this.canAdmin || (!!uid && r.absentUserId === uid);
  }

  submit(): void {
    if (!this.canCreate || this.form.invalid || this.submitting) {
      return;
    }
    const uid = this.myUserId;
    if (!uid) {
      this.toast.error('acting.noUserId');
      return;
    }
    const v = this.form.getRawValue();
    const absentId = this.canAdmin ? (v.absentUserId as string)?.trim() : uid;
    if (!absentId) {
      this.toast.error('acting.absentRequired');
      return;
    }
    const deptRaw = (v.departmentId as string)?.trim();
    const departmentId =
      deptRaw === '' || deptRaw == null ? null : Number.parseInt(deptRaw, 10);
    if (departmentId != null && Number.isNaN(departmentId)) {
      this.toast.error('acting.badDeptId');
      return;
    }

    this.submitting = true;
    this.lastCreatedId = null;
    this.api
      .create({
        absentUserId: absentId,
        actingUserId: v.actingUserId!.trim(),
        departmentId,
        includeDepartmentSubtree: !!v.includeDepartmentSubtree,
        processDefinitionKey: (v.processDefinitionKey as string)?.trim() || null,
        taskDefinitionKey: (v.taskDefinitionKey as string)?.trim() || null,
        validFrom: v.validFrom!,
        validTo: v.validTo!,
        notes: (v.notes as string)?.trim() || null
      })
      .subscribe({
        next: (created) => {
          this.submitting = false;
          this.lastCreatedId = created?.id ?? null;
          this.form.patchValue({
            actingUserId: '',
            departmentId: '',
            includeDepartmentSubtree: false,
            processDefinitionKey: '',
            taskDefinitionKey: '',
            validFrom: '',
            validTo: '',
            notes: ''
          });
          if (this.canAdmin) {
            this.form.patchValue({ absentUserId: '' });
          }
          this.toast.success('acting.createSuccess');
          this.refresh();
          if (this.activeTab === 'audit' && this.auditLoaded) {
            this.auditLoaded = false;
            this.loadAudit();
          }
        },
        error: (err) => {
          this.submitting = false;
          const msg = err?.error?.message ?? err?.error?.detail ?? null;
          if (msg) {
            this.toast.errorRaw(msg);
          } else {
            this.toast.error('acting.createFailed');
          }
        }
      });
  }

  confirmRevoke(row: ActingAssignmentDto): void {
    this.dialogService
      .openConfirm({
        titleKey: 'acting.revokeConfirmTitle',
        messageKey: 'acting.revokeConfirmMessage',
        confirmButton: { labelKey: 'acting.revokeConfirm', color: 'warn' },
        cancelButton: { labelKey: 'common.close' }
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.revoke(row.id).subscribe({
          next: () => {
            this.toast.success('acting.revokeSuccess');
            this.refresh();
            if (this.activeTab === 'audit' && this.auditLoaded) {
              this.auditLoaded = false;
              this.loadAudit();
            }
          },
          error: (err) => {
            const msg = err?.error?.message ?? err?.error?.detail ?? null;
            if (msg) {
              this.toast.errorRaw(msg);
            } else {
              this.toast.error('acting.revokeFailed');
            }
          }
        });
      });
  }

  userLabel(u: UserListDto): string {
    const name = this.i18n.currentLang() === 'en' ? u.fullNameEn : u.fullNameAr;
    return `${name} (${u.username})`;
  }
}
