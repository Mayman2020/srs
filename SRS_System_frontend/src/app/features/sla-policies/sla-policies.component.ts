import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SlaPolicyApiService } from '../../core/api/sla-policy-api.service';
import { OrgRoutingApiService } from '../../core/api/org-routing-api.service';
import {
  CreateSlaEscalationStepRequestDto,
  CreateSlaPolicyRequestDto,
  SlaBreachEventDto,
  SlaEscalationActionTypeDto,
  SlaPolicyDto
} from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

/**
 * Slice 3 — SLA Policy admin screen. Tab 1: policies (CRUD). Tab 2: breach ledger (read-only).
 *
 * Mutations are gated server-side by `SLA_POLICY_MANAGE`; the route guard is set to the same
 * permission, so a `SLA_POLICY_VIEW`-only auditor reaching this URL directly will be redirected
 * to /dashboard by `permissionCanMatch`. The breach tab fetches `?onlyActive=true` by default
 * because the operational concern is "what is still overdue right now?".
 */
@Component({
  selector: 'app-sla-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './sla-policies.component.html',
  styleUrl: './sla-policies.component.css'
})
export class SlaPoliciesComponent implements OnInit {
  policies: SlaPolicyDto[] = [];
  breaches: SlaBreachEventDto[] = [];
  loading = true;
  submitting = false;
  activeTab: 'policies' | 'breaches' = 'policies';
  editingId: number | null = null;
  showOnlyActiveBreaches = true;

  actionCodes: SlaEscalationActionTypeDto[] = [];
  orgLevels: string[] = [''];

  readonly form: FormGroup;

  constructor(
    private readonly api: SlaPolicyApiService,
    private readonly orgApi: OrgRoutingApiService,
    private readonly fb: FormBuilder,
    private readonly i18n: I18nService,
    private readonly dialogService: DialogService,
    private readonly toast: NotificationService
  ) {
    this.form = this.fb.group({
      code: ['', Validators.required],
      nameAr: ['', Validators.required],
      nameEn: ['', Validators.required],
      description: [''],
      orgLevelCode: [''],
      targetHours: [24, [Validators.required, Validators.min(1)]],
      breachGraceMinutes: [0, [Validators.min(0)]],
      active: [true],
      steps: this.fb.array([])
    });
  }

  ngOnInit(): void {
    forkJoin({
      actions: this.api.listEscalationActions(),
      levels: this.orgApi.listLevels()
    }).subscribe({
      next: ({ actions, levels }) => {
        this.actionCodes = actions ?? [];
        this.orgLevels = ['', ...(levels ?? []).map((l) => l.code)];
        this.refresh();
      },
      error: () => {
        this.actionCodes = [];
        this.orgLevels = [''];
        this.refresh();
      }
    });
  }

  refresh(): void {
    this.loading = true;
    this.api.list().subscribe({
      next: (rows) => {
        this.policies = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.policies = [];
        this.loading = false;
        this.toast.error('sla.loadFailed');
      }
    });
    this.refreshBreaches();
  }

  refreshBreaches(): void {
    this.api.listBreaches(this.showOnlyActiveBreaches).subscribe({
      next: (rows) => (this.breaches = rows ?? []),
      error: () => {
        this.breaches = [];
        this.toast.error('sla.loadBreachesFailed');
      }
    });
  }

  setTab(tab: 'policies' | 'breaches'): void {
    this.activeTab = tab;
    if (tab === 'breaches') this.refreshBreaches();
  }

  toggleBreachFilter(): void {
    this.showOnlyActiveBreaches = !this.showOnlyActiveBreaches;
    this.refreshBreaches();
  }

  startCreate(): void {
    this.editingId = null;
    this.form.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      description: '',
      orgLevelCode: '',
      targetHours: 24,
      breachGraceMinutes: 0,
      active: true
    });
    this.steps.clear();
    this.addStep(0, 'NOTIFY_MANAGER', 0);
  }

  startEdit(p: SlaPolicyDto): void {
    this.editingId = p.id;
    this.form.reset({
      code: p.code,
      nameAr: p.nameAr,
      nameEn: p.nameEn,
      description: p.description ?? '',
      orgLevelCode: p.orgLevelCode ?? '',
      targetHours: p.targetHours,
      breachGraceMinutes: p.breachGraceMinutes,
      active: p.active
    });
    this.steps.clear();
    for (const step of p.steps ?? []) {
      this.addStep(
        step.stepOrder,
        (step.actionCode as SlaEscalationActionCode) ?? 'NOTIFY_MANAGER',
        step.delayAfterBreachMinutes,
        step.description ?? ''
      );
    }
    if (!p.steps || p.steps.length === 0) {
      this.addStep(0, 'NOTIFY_MANAGER', 0);
    }
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      description: '',
      orgLevelCode: '',
      targetHours: 24,
      breachGraceMinutes: 0,
      active: true
    });
    this.steps.clear();
  }

  get steps(): FormArray<FormGroup> {
    return this.form.get('steps') as FormArray<FormGroup>;
  }

  addStep(
    stepOrder: number = this.steps.length,
    actionCode: SlaEscalationActionCode = 'NOTIFY_MANAGER',
    delay: number = 30,
    description: string = ''
  ): void {
    this.steps.push(
      this.fb.group({
        stepOrder: [stepOrder, [Validators.required, Validators.min(0)]],
        actionCode: [actionCode as SlaEscalationActionCode, Validators.required],
        delayAfterBreachMinutes: [delay, [Validators.required, Validators.min(0)]],
        description: [description]
      })
    );
  }

  removeStep(idx: number): void {
    this.steps.removeAt(idx);
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    const stepDtos: CreateSlaEscalationStepRequestDto[] = (v.steps ?? []).map((s: any) => ({
      stepOrder: s.stepOrder,
      actionCode: s.actionCode,
      delayAfterBreachMinutes: s.delayAfterBreachMinutes,
      description: s.description ? s.description : null
    }));
    const body: CreateSlaPolicyRequestDto = {
      code: (v.code ?? '').trim(),
      nameAr: (v.nameAr ?? '').trim(),
      nameEn: (v.nameEn ?? '').trim(),
      description: v.description ? v.description.trim() : null,
      orgLevelCode: v.orgLevelCode ? v.orgLevelCode : null,
      targetHours: v.targetHours,
      breachGraceMinutes: v.breachGraceMinutes ?? 0,
      active: v.active,
      steps: stepDtos
    };
    const action$ = this.editingId == null
      ? this.api.create(body)
      : this.api.update(this.editingId, body);
    action$.subscribe({
      next: () => {
        this.submitting = false;
        this.toast.success(this.editingId == null ? 'sla.createSuccess' : 'sla.updateSuccess');
        this.cancelEdit();
        this.refresh();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err?.error?.message ?? err?.error?.detail ?? null;
        if (msg) {
          this.toast.errorRaw(msg);
        } else {
          this.toast.error('sla.saveFailed');
        }
      }
    });
  }

  confirmDelete(p: SlaPolicyDto): void {
    this.dialogService
      .openConfirm({
        titleKey: 'sla.deleteConfirmTitle',
        messageKey: 'sla.deleteConfirmMessage',
        confirmButton: { labelKey: 'sla.deleteConfirm', color: 'warn' },
        cancelButton: { labelKey: 'common.close' }
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.api.delete(p.id).subscribe({
          next: () => {
            this.toast.success('sla.deleteSuccess');
            if (this.editingId === p.id) this.cancelEdit();
            this.refresh();
          },
          error: () => this.toast.error('sla.deleteFailed')
        });
      });
  }

  policyName(p: SlaPolicyDto): string {
    return this.i18n.currentLang() === 'en' ? p.nameEn : p.nameAr;
  }

  actionLabel(action: SlaEscalationActionTypeDto): string {
    return this.i18n.currentLang() === 'en' ? action.nameEn : action.nameAr;
  }
}
