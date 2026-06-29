import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TaskDelegationApiService } from '../../core/api/task-delegation-api.service';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import {
  TaskDelegationDto,
  TaskDelegationListDto,
  TaskDelegationScope,
  UserListDto
} from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { DateFieldComponent } from '../../shared/components/date-field/date-field.component';

/**
 * Active task-delegations screen (Slice 2). Three tabs:
 * - Outgoing active: rows where the current user is the delegator.
 * - Incoming active: rows where the current user is the delegate (the "acting for" surface).
 * - Inactive: revoked or expired rows from either side, kept for traceability.
 *
 * The form on the page creates new task delegations. Errors from the backend (overlap, cycle,
 * clearance, etc.) are surfaced via the toast service — never silently swallowed.
 */
@Component({
  selector: 'app-task-delegations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    ErpAutoReferenceFieldComponent,
    DateFieldComponent
  ],
  templateUrl: './task-delegations.component.html',
  styleUrl: './task-delegations.component.css'
})
export class TaskDelegationsComponent implements OnInit {
  list: TaskDelegationListDto = { outgoingActive: [], incomingActive: [], inactive: [] };
  users: UserListDto[] = [];
  loading = true;
  submitting = false;

  /** Last create call result (shown to the user as a confirmation reference). */
  lastCreatedDelegationId: string | null = null;

  /** Tab id: 'outgoing' | 'incoming' | 'inactive'. */
  activeTab: 'outgoing' | 'incoming' | 'inactive' = 'outgoing';

  readonly form;

  readonly scopeOptions: TaskDelegationScope[] = ['TASK', 'TYPE_CONFIDENTIALITY'];

  constructor(
    private readonly api: TaskDelegationApiService,
    private readonly usersApi: UserDirectoryApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly dialogService: DialogService,
    private readonly toast: NotificationService
  ) {
    this.form = this.fb.group({
      delegateUserId: ['', Validators.required],
      scopeType: ['TASK' as TaskDelegationScope, Validators.required],
      camundaTaskId: [''],
      correspondenceId: [''],
      allowedCorrespondenceTypeCodes: [''],
      allowedConfidentialityCodes: [''],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
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
    this.api.listMine().subscribe({
      next: (list) => {
        this.list = list ?? { outgoingActive: [], incomingActive: [], inactive: [] };
        this.loading = false;
      },
      error: () => {
        this.list = { outgoingActive: [], incomingActive: [], inactive: [] };
        this.loading = false;
        this.toast.error('taskDelegations.loadFailed');
      }
    });
  }

  setTab(tab: 'outgoing' | 'incoming' | 'inactive'): void {
    this.activeTab = tab;
  }

  currentRows(): TaskDelegationDto[] {
    if (this.activeTab === 'outgoing') return this.list.outgoingActive;
    if (this.activeTab === 'incoming') return this.list.incomingActive;
    return this.list.inactive;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    this.lastCreatedDelegationId = null;
    const v = this.form.getRawValue();
    this.api
      .create({
        delegateUserId: v.delegateUserId!,
        scopeType: v.scopeType! as TaskDelegationScope,
        camundaTaskId: v.camundaTaskId?.trim() || null,
        correspondenceId: v.correspondenceId?.trim() || null,
        allowedCorrespondenceTypeCodes: v.allowedCorrespondenceTypeCodes?.trim() || null,
        allowedConfidentialityCodes: v.allowedConfidentialityCodes?.trim() || null,
        validFrom: v.validFrom!,
        validTo: v.validTo!,
        notes: v.notes?.trim() || null
      })
      .subscribe({
        next: (created) => {
          this.submitting = false;
          this.lastCreatedDelegationId = created?.id ?? null;
          this.form.reset({ scopeType: 'TASK' as TaskDelegationScope });
          this.toast.success('taskDelegations.createSuccess');
          this.refresh();
        },
        error: (err) => {
          this.submitting = false;
          // Backend emits a ProblemDetail / ResponseEnvelope on 4xx — surface the message so the
          // user understands which guard rejected (overlap / cycle / clearance / dates).
          const msg = err?.error?.message ?? err?.error?.detail ?? null;
          if (msg) {
            this.toast.errorRaw(msg);
          } else {
            this.toast.error('taskDelegations.createFailed');
          }
        }
      });
  }

  confirmRevoke(row: TaskDelegationDto): void {
    this.dialogService
      .openConfirm({
        titleKey: 'taskDelegations.revokeConfirmTitle',
        messageKey: 'taskDelegations.revokeConfirmMessage',
        confirmButton: { labelKey: 'taskDelegations.revokeConfirm', color: 'warn' },
        cancelButton: { labelKey: 'common.close' }
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.revoke(row.id).subscribe({
          next: () => {
            this.toast.success('taskDelegations.revokeSuccess');
            this.refresh();
          },
          error: (err) => {
            const msg = err?.error?.message ?? err?.error?.detail ?? null;
            if (msg) {
              this.toast.errorRaw(msg);
            } else {
              this.toast.error('taskDelegations.revokeFailed');
            }
          }
        });
      });
  }

  userLabel(u: UserListDto): string {
    const name = this.i18n.currentLang() === 'en' ? u.fullNameEn : u.fullNameAr;
    return `${name} (${u.username})`;
  }

  delegatorName(d: TaskDelegationDto): string {
    return this.i18n.currentLang() === 'en' ? d.delegator.fullNameEn : d.delegator.fullNameAr;
  }

  delegateName(d: TaskDelegationDto): string {
    return this.i18n.currentLang() === 'en' ? d.delegate.fullNameEn : d.delegate.fullNameAr;
  }

  scopeLabel(scope: TaskDelegationScope): string {
    return scope === 'TASK'
      ? this.i18n.instant('taskDelegations.scopeTask')
      : this.i18n.instant('taskDelegations.scopeTypeConfidentiality');
  }

  statusLabel(d: TaskDelegationDto): string {
    if (d.revokedAt && d.revokedBy) {
      return this.i18n.instant('taskDelegations.statusRevoked');
    }
    if (d.revokedAt && !d.revokedBy) {
      return this.i18n.instant('taskDelegations.statusExpired');
    }
    return this.i18n.instant('taskDelegations.statusActive');
  }
}
