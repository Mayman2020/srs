import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import {
  MultiChoiceId,
  MultiChoiceOption,
  MultiChoiceTableComponent
} from '../../shared/components/multi-choice-table/multi-choice-table.component';
import { GenericSelectComponent } from '../../shared/components/generic-select/generic-select.component';
import {
  RegistrationDeskApiService,
  RegistrationDeskMode,
  RegistrationDeskRowDto
} from '../../core/api/registration-desk-api.service';
import { OrganizationApiService } from '../../core/api/organization-api.service';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { OrganizationFlatDto, DepartmentFlatDto } from '../../core/api/api-types';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';
import { CorrespondenceApiService } from '../../core/api/correspondence-api.service';
import { Router } from '@angular/router';
import {
  BarcodeLabelDialogComponent,
  BarcodeLabelItem
} from '../../shared/dialogs/barcode-label-dialog.component';

@Component({
  selector: 'app-registration-desk',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    LookupTranslatePipe,
    ErpPageShellComponent,
    SrsDataTableComponent,
    MultiChoiceTableComponent,
    GenericSelectComponent
  ],
  templateUrl: './registration-desk.component.html',
  styleUrl: './registration-desk.component.scss'
})
export class RegistrationDeskComponent implements OnInit {
  private readonly api = inject(RegistrationDeskApiService);
  private readonly orgApi = inject(OrganizationApiService);
  private readonly deptApi = inject(DepartmentApiService);
  private readonly lookupLabels = inject(LookupLabelsService);
  private readonly fb = inject(FormBuilder);
  private readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly correspondenceApi = inject(CorrespondenceApiService);
  private readonly router = inject(Router);

  scanValue = '';
  scanning = false;

  deskMode: RegistrationDeskMode = 'INBOUND';
  todayRows: RegistrationDeskRowDto[] = [];
  loadingToday = true;
  submitting = false;
  organizations: OrganizationFlatDto[] = [];
  handoffOptions: MultiChoiceOption[] = [];
  priorityLevels: { key: string }[] = [];
  confidentialityLevels: { key: string }[] = [];
  classificationLevels: { key: string }[] = [];
  selectedHandoffIds: MultiChoiceId[] = [];
  lastIntakeBarcode: string | null = null;

  readonly intakeForm = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(500)]],
    externalReferenceNumber: ['', Validators.maxLength(128)],
    senderOrganizationId: [null as number | null],
    priorityCode: ['NORMAL', Validators.required],
    confidentialityCode: ['INTERNAL', Validators.required],
    classificationCode: ['ADMIN', Validators.required],
    description: ['', Validators.maxLength(20000)]
  });

  ngOnInit(): void {
    forkJoin({
      orgs: this.orgApi.list(),
      depts: this.deptApi.list(),
      priorities: this.lookupLabels.loadTable(LookupCode.Priority),
      confidentialities: this.lookupLabels.loadTable(LookupCode.Confidentiality),
      classifications: this.lookupLabels.loadTable(LookupCode.Classification)
    }).subscribe({
      next: ({ orgs, depts, priorities, confidentialities, classifications }) => {
        this.organizations = orgs ?? [];
        this.handoffOptions = (depts ?? []).map((d) => ({
          id: d.id,
          label: this.deptLabel(d),
          code: d.code
        }));
        this.priorityLevels = (priorities ?? []).map((p) => ({ key: p.code }));
        this.confidentialityLevels = (confidentialities ?? []).map((c) => ({ key: c.code }));
        this.classificationLevels = (classifications ?? []).map((c) => ({ key: c.code }));
      }
    });
    this.loadToday();
  }

  setDeskMode(mode: RegistrationDeskMode): void {
    if (this.deskMode === mode) {
      return;
    }
    this.deskMode = mode;
    this.loadToday();
  }

  loadToday(): void {
    this.loadingToday = true;
    this.api.today(this.deskMode).subscribe({
      next: (rows) => {
        this.todayRows = rows ?? [];
        this.loadingToday = false;
      },
      error: () => {
        this.todayRows = [];
        this.loadingToday = false;
        this.notification.error('registrationDesk.loadTodayFailed');
      }
    });
  }

  onHandoffChange(ids: MultiChoiceId[]): void {
    this.selectedHandoffIds = ids;
  }

  submitIntake(): void {
    if (this.intakeForm.invalid) {
      this.intakeForm.markAllAsTouched();
      return;
    }
    if (!this.selectedHandoffIds.length) {
      this.notification.warning('registrationDesk.handoffRequired');
      return;
    }
    const v = this.intakeForm.getRawValue();
    this.submitting = true;
    this.api
      .intake({
        deskMode: this.deskMode,
        subject: v.subject.trim(),
        priorityCode: v.priorityCode,
        confidentialityCode: v.confidentialityCode,
        classificationCode: v.classificationCode,
        description: v.description.trim() || null,
        externalReferenceNumber: v.externalReferenceNumber.trim() || null,
        senderOrganizationId: v.senderOrganizationId,
        handoffDepartmentIds: this.selectedHandoffIds.map((id) => Number(id))
      })
      .subscribe({
        next: (res) => {
          this.submitting = false;
          this.lastIntakeBarcode = res.barcodeValue ?? res.referenceNumber;
          this.notification.success('registrationDesk.intakeSuccess');
          this.intakeForm.patchValue({
            subject: '',
            externalReferenceNumber: '',
            description: ''
          });
          this.selectedHandoffIds = [];
          this.loadToday();
          this.openBarcodeDialog([
            { reference: res.barcodeValue ?? res.referenceNumber, subject: v.subject.trim() }
          ]);
        },
        error: () => {
          this.submitting = false;
          this.notification.error('registrationDesk.intakeFailed');
        }
      });
  }

  openBatchBarcodePrint(): void {
    const items: BarcodeLabelItem[] = this.todayRows.map((r) => ({
      reference: r.barcodeValue || r.referenceNumber,
      subject: r.subject
    }));
    if (!items.length) {
      this.notification.warning('registrationDesk.noLabels');
      return;
    }
    this.openBarcodeDialog(items);
  }

  printRow(row: RegistrationDeskRowDto): void {
    this.openBarcodeDialog([
      { reference: row.barcodeValue || row.referenceNumber, subject: row.subject }
    ]);
  }

  onScanSubmit(ev: Event): void {
    ev.preventDefault();
    const code = this.scanValue.trim();
    if (!code || this.scanning) {
      return;
    }
    this.scanning = true;
    this.correspondenceApi.getByBarcode(code).subscribe({
      next: (detail) => {
        this.scanning = false;
        this.scanValue = '';
        void this.router.navigate(['/correspondence', detail.id]);
      },
      error: () => {
        this.scanning = false;
        this.notification.warning('registrationDesk.scanNotFound');
      }
    });
  }

  orgLabel(o: OrganizationFlatDto): string {
    return this.i18n.currentLang() === 'en' ? o.nameEn : o.nameAr;
  }

  private deptLabel(d: DepartmentFlatDto): string {
    return this.i18n.currentLang() === 'en' ? d.nameEn : d.nameAr;
  }

  private openBarcodeDialog(items: BarcodeLabelItem[]): void {
    this.dialog.open(BarcodeLabelDialogComponent, {
      width: 'min(420px, 94vw)',
      data: { items }
    });
  }
}
