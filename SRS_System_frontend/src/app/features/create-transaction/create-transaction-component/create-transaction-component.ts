import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, NgZone, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormArray,
  FormsModule
} from '@angular/forms';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { AngularEditorConfig } from '@kolkov/angular-editor';


// Angular Material
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TransactionService } from '../../../services/transaction.service';
import { LookupService } from '../../../core/api/lookup.service';
import { AttachmentApiService } from '../../../core/api/attachment-api.service';
import { DepartmentApiService } from '../../../core/api/department-api.service';
import {
  CorrespondenceAttachmentFormDto,
  CorrespondenceCreateRequest,
  LetterTemplateDto
} from '../../../core/api/api-types';
import { LetterTemplateApiService } from '../../../core/api/letter-template-api.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../../core/i18n/lookup-translate.pipe';
import { LookupLabelsService } from '../../../core/lookup/lookup-labels.service';
import { GenericSelectComponent } from '../../../component/generic-select/generic-select.component';
import { EditorModule } from '@tinymce/tinymce-angular';
import { DepartmentTreeDialogComponent } from '../department-tree-dialog/department-tree-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import JsBarcode from 'jsbarcode';

type LetterTemplateItem = {
  key: string;
  nameAr?: string;
  nameEn?: string;
  getHtml: () => string;
};

@Component({
  selector: 'app-transaction-create',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    AngularEditorModule,
    MatStepperModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    GenericSelectComponent,
    EditorModule,
    TranslatePipe,
    LookupTranslatePipe
  ],
  templateUrl: './create-transaction-component.html',
  styleUrls: ['./create-transaction-component.css']
})
export class CreateTransactionComponent implements OnInit {

  @ViewChild('barcode', { static: false }) barcode!: ElementRef;
  currentStep = 1;
  totalSteps = 5;

  readonly wizardStepKeys = [
    'createTx.wizard.step1',
    'createTx.wizard.step2',
    'createTx.wizard.step3',
    'createTx.wizard.step4',
    'createTx.wizard.step5'
  ];

  attachments: {
    file: File;
    name: string;
    description: string;
  }[] = [];
  transactionNumber = '';
  lastCreatedCorrespondenceId: string | null = null;

  private deptLabels = new Map<number, string>();

  ngOnInit(): void {
    this.editorConfig = {
      ...this.editorConfig,
      placeholder: this.i18n.instant('createTx.letterEditor.contentPlaceholder')
    };

    this.letterTemplates = this.buildLocalLetterTemplates();

    forkJoin({
      bundle: this.lookupService.getBundle(),
      templates: this.letterTemplateApi.list().pipe(
        catchError((err: unknown) => {
          console.error('[CreateTransaction] letter templates load failed', err);
          return of([] as LetterTemplateDto[]);
        })
      ),
    }).subscribe({
      next: ({ bundle, templates }) => {
        this.lookupLabels.hydrateFromBundle(bundle);
        this.transactionTypes = bundle.correspondenceTypes.map((t) => ({ key: t.code }));
        this.secrecyLevels = bundle.confidentialities.map((c) => ({ key: c.code }));
        this.priorityLevels = bundle.priorities.map((p) => ({ key: p.code }));
        this.classificationLevels = (bundle.classifications ?? []).map((c) => ({ key: c.code }));
        this.applyLetterTemplatesFromApi(templates ?? []);
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        console.error('[CreateTransaction] lookup bundle load failed', err);
        const msg = err.userMessage ?? this.i18n.instant('errors.generic');
        this.snackBar.open(msg, this.i18n.instant('common.close'), { duration: 6000 });
        this.transactionTypes = [];
        this.secrecyLevels = [];
        this.priorityLevels = [];
        this.classificationLevels = [];
        this.applyLetterTemplatesFromApi([]);
      },
    });

    this.departmentApi.list().subscribe({
      next: (rows) => {
        const lang = this.i18n.currentLang();
        this.deptLabels.clear();
        for (const r of rows ?? []) {
          const label = lang === 'en' ? r.nameEn : r.nameAr;
          this.deptLabels.set(r.id, label);
        }
      },
      error: () => this.deptLabels.clear()
    });
  }

  generateBarcode() {

    setTimeout(() => {

      if (this.barcode?.nativeElement && this.transactionNumber) {

        JsBarcode(this.barcode.nativeElement, this.transactionNumber, {
          format: "CODE128",
          width: 2,
          height: 60,
          displayValue: false
        });

      }

    });

  }

  goToStep(step: number) {
    if (step <= this.totalSteps) {
      this.currentStep = step;

      if (step === 5) {
        this.generateBarcode();
      }
    }
  }

  /** Stepper header: allow going back only; forward navigation uses {@link #nextStep}. */
  goToStepIfAllowed(step: number): void {
    if (step < 1 || step > this.totalSteps) {
      return;
    }
    if (step < this.currentStep) {
      this.goToStep(step);
    }
  }

  nextStep() {
    if (this.currentStep === 1) {
      this.basicForm.markAllAsTouched();
      if (this.basicForm.invalid) {
        this.showNotification(this.i18n.instant('createTx.validation.step1'), 'error');
        return;
      }
      this.currentStep++;
      return;
    }
    if (this.currentStep === 2) {
      this.secondaryForm.markAllAsTouched();
      if (this.secondaryForm.invalid) {
        this.showNotification(this.i18n.instant('createTx.validation.step2Form'), 'error');
        return;
      }
      if (this.toArray.length === 0) {
        this.showNotification(this.i18n.instant('createTx.validation.recipientsRequired'), 'error');
        return;
      }
      this.currentStep++;
      return;
    }
    if (this.currentStep === 3) {
      if (this.selectedTemplateKey === 'no-letter') {
        this.currentStep++;
        return;
      }
      this.letterForm.markAllAsTouched();
      const content = (this.letterForm.value.letterContent ?? '').toString().trim();
      if (this.letterForm.invalid || !content) {
        this.showNotification(this.i18n.instant('createTx.validation.step3'), 'error');
        return;
      }
      this.currentStep++;
      return;
    }
    if (this.currentStep === 4) {
      this.submit();
      return;
    }
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  previousStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  getProgressWidth(): string {
    return ((this.currentStep - 1) / (this.totalSteps - 1)) * 100 + '%';
  }


  basicForm: FormGroup;
  secondaryForm: FormGroup;
  letterForm: FormGroup;

  editorInit: any;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
    private attachmentApi: AttachmentApiService,
    private departmentApi: DepartmentApiService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    private lookupService: LookupService,
    private letterTemplateApi: LetterTemplateApiService,
    private i18n: I18nService,
    private lookupLabels: LookupLabelsService
  ) {

    // Step 1
    this.basicForm = this.fb.group({
      type: ['', Validators.required],
      secrecy: ['', Validators.required],
      priority: ['', Validators.required],
      classification: ['', Validators.required],
      subject: ['', [Validators.required, Validators.minLength(5)]],
      description: ['']
    });

    // Step 2 — recipients optional at API level; owner department taken from first selection when present
    this.secondaryForm = this.fb.group({
      from: [''],
      to: this.fb.array<number>([]),
      cc: this.fb.array<number>([]),
      maxDays: [5, [Validators.required, Validators.min(1), Validators.max(30)]],
      workflowFirstAssigneeUserId: [''],
      workflowFirstCandidateGroup: ['']
    });

    // Step 3
    this.letterForm = this.fb.group({
      letterContent: [this.defaultTemplate(), Validators.required]
    });

    // TinyMCE Config
    this.editorInit = {
      language: 'ar',
      directionality: 'rtl',
      height: 600,
      menubar: false,
      branding: false,
      statusbar: true,
      resize: true,
      plugins: [
        'lists', 'link', 'table', 'code', 'fullscreen', 'wordcount'
      ],
      toolbar:
        'undo redo | formatselect | bold italic underline | ' +
        'alignright aligncenter alignleft | ' +
        'bullist numlist | table | removeformat code fullscreen',
      font_family_formats:
        'Cairo=Cairo, sans-serif;' +
        'Tajawal=Tajawal, sans-serif;' +
        'Arial=arial,helvetica,sans-serif',
      content_style: `
        @import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap');
        body {
          font-family: 'Cairo', sans-serif;
          font-size: 15px;
          line-height: 2;
          color: #1a1a1a;
          direction: rtl;
          padding: 40px;
          max-width: 210mm;
          margin: 0 auto;
        }
        h1, h2, h3 { color: #0B6E4F; font-weight: 700; }
        .label { font-weight: 600; color: #0B6E4F; }
      `
    };
  }

  selectedTemplateKey: string = 'default';


  editorConfig: AngularEditorConfig = {
    editable: true,
    spellcheck: true,
    height: 'auto',
    minHeight: '800px',
    placeholder: '',
    translate: 'no',
    defaultParagraphSeparator: 'p',
    defaultFontName: 'Cairo',
    sanitize: false,
    toolbarHiddenButtons: [
      ['insertImage', 'insertVideo']
    ],
    fonts: [
      { class: 'cairo', name: 'Cairo' }
    ]
  };

  onTemplateChange(key: string) {
    this.selectedTemplateKey = key;

    const template = this.letterTemplates.find((t) => t.key === key);
    if (!template) return;

    const ctl = this.letterForm.get('letterContent');
    if (key === 'no-letter') {
      ctl?.clearValidators();
    } else {
      ctl?.setValidators([Validators.required]);
    }
    ctl?.updateValueAndValidity();

    this.letterForm.patchValue({
      letterContent: template.getHtml()
    });
  }
  /* ================================
     LETTER TEMPLATES SYSTEM (Premium)
  ================================ */

  buildBaseTemplate(bodyContent: string): string {
    const locale = this.i18n.currentLang() === 'en' ? 'en-GB' : 'ar-EG';
    const today = new Date().toLocaleDateString(locale, {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });

    return `
<div style="
  max-width:900px;
  margin:0 auto;
  padding:80px 90px;
  background:#ffffff;
  box-shadow:0 15px 40px rgba(0,0,0,.08);
  border-radius:14px;
  direction:rtl;
  font-family:'Cairo',sans-serif;
  line-height:2.4;
  color:#1f2937;
">

  <div style="text-align:center;margin-bottom:40px;">
    <h1 style="
      color:#0B6E4F;
      font-size:34px;
      font-weight:800;
      margin-bottom:10px;
    ">
      ${this.i18n.instant('createTx.letterEditor.headerEntity')}
    </h1>

    <div style="color:#6b7280;font-size:16px;">
      ${this.i18n.instant('createTx.letterEditor.headerSubtitle')}
    </div>
  </div>

  <div style="
    height:3px;
    background:#0B6E4F;
    margin:30px 0 40px 0;
  "></div>

  <div style="
    background:#f3f4f6;
    padding:25px;
    border-radius:14px;
    margin-bottom:50px;
    font-size:16px;
  ">
    <div style="margin-bottom:10px;">
      <strong style="color:#0B6E4F">${this.i18n.instant('createTx.letterEditor.dateLabel')}</strong> ${today}
    </div>
    <div>
      <strong style="color:#0B6E4F">${this.i18n.instant('createTx.summary.ref')}</strong> ${this.i18n.instant('createTx.letterEditor.refFilledPlain')}
    </div>
  </div>

  ${bodyContent}

  <div style="margin-top:120px;">
    <div style="font-weight:700;color:#0B6E4F;margin-bottom:14px;">
      ${this.i18n.instant('createTx.letterEditor.footerManager')}
    </div>

    <div style="
      width:260px;
      border-bottom:2px solid #9ca3af;
      margin-top:30px;
    "></div>

    <div style="margin-top:10px;color:#6b7280;">
      ${this.i18n.instant('createTx.letterEditor.footerSignature')}
    </div>
  </div>

</div>
`;
  }
  /* ================================
     DEFAULT TEMPLATE
  ================================ */
  defaultTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('createTx.letterEditor.toLabel')}</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('transactions.subject')}:</span> .......................................................</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.salutation')}
    </p>

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.default.receiptBefore')}
      <strong>${this.i18n.instant('createTx.letterBody.default.topicBracket')}</strong>${this.i18n.instant('createTx.letterBody.default.receiptAfter')}
    </p>

    <ol style="margin: 20px 0; padding-right: 40px; line-height: 2.2;">
      <li>${this.i18n.instant('createTx.letterBody.listDots')}</li>
      <li>${this.i18n.instant('createTx.letterBody.listDots')}</li>
      <li>${this.i18n.instant('createTx.letterBody.listDots')}</li>
    </ol>

    <p>${this.i18n.instant('createTx.letterBody.default.thanks')}</p>
  `);
  }

  /* ================================
     REMINDER
  ================================ */
  reminderTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('createTx.letterEditor.toLabel')}</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('transactions.subject')}:</span> ${this.i18n.instant('createTx.letterBody.reminder.subjectLine')}</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.salutation')}
    </p>

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.reminder.bodyBeforeRef')}<strong>${this.i18n.instant('createTx.letterEditor.refPlaceholderBracket')}</strong>${this.i18n.instant('createTx.letterBody.reminder.bodyAfterRef')}
    </p>

    <p>${this.i18n.instant('createTx.letterBody.reminder.thanks')}</p>
  `);
  }

  /* ================================
     APPROVAL
  ================================ */
  approvalTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('createTx.letterEditor.toLabel')}</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('transactions.subject')}:</span> ${this.i18n.instant('createTx.letterBody.approval.subjectLine')}</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.salutation')}
    </p>

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.approval.bodyBefore')}
      <strong>${this.i18n.instant('createTx.letterBody.approval.topicBracket')}</strong>${this.i18n.instant('createTx.letterBody.approval.bodyAfter')}
    </p>
  `);
  }

  /* ================================
     REJECTION
  ================================ */
  rejectionTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('createTx.letterEditor.toLabel')}</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">${this.i18n.instant('transactions.subject')}:</span> ${this.i18n.instant('createTx.letterBody.rejection.subjectLine')}</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.salutation')}
    </p>

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.rejection.bodyBefore')}
      <strong>${this.i18n.instant('createTx.letterBody.rejection.reasonBracket')}</strong>${this.i18n.instant('createTx.letterBody.rejection.bodyAfter')}
    </p>
  `);
  }

  /* ================================
     ADMINISTRATIVE CIRCULAR
  ================================ */
  administrativeCircularTemplate(): string {
    return this.buildBaseTemplate(`
    <p style="text-align:center;font-weight:700;font-size:18px;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.adminCircular.title')}
    </p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.adminCircular.bodyBefore')}
      <strong>${this.i18n.instant('createTx.letterBody.adminCircular.topicBracket')}</strong>${this.i18n.instant('createTx.letterBody.adminCircular.bodyAfter')}
    </p>
  `);
  }

  /* ================================
     MINISTERIAL CIRCULAR
  ================================ */
  ministerialCircularTemplate(): string {
    return this.buildBaseTemplate(`
    <p style="text-align:center;font-weight:700;font-size:18px;color:#0B6E4F;">
      ${this.i18n.instant('createTx.letterBody.ministerialCircular.title')}
    </p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="line-height:2.2;text-align:justify;">
      ${this.i18n.instant('createTx.letterBody.ministerialCircular.bodyBefore')}
      <strong>${this.i18n.instant('createTx.letterBody.ministerialCircular.topicBracket')}</strong>${this.i18n.instant('createTx.letterBody.ministerialCircular.bodyAfter')}
    </p>
  `);
  }

  /* ================================
     TEMPLATES LIST
  ================================ */
  letterTemplates: LetterTemplateItem[] = [];

  private buildLocalLetterTemplates(): LetterTemplateItem[] {
    return [
      { key: 'default', getHtml: () => this.defaultTemplate() },
      { key: 'reminder', getHtml: () => this.reminderTemplate() },
      { key: 'approval', getHtml: () => this.approvalTemplate() },
      { key: 'rejection', getHtml: () => this.rejectionTemplate() },
      { key: 'admin-circular', getHtml: () => this.administrativeCircularTemplate() },
      { key: 'ministerial-circular', getHtml: () => this.ministerialCircularTemplate() },
      { key: 'no-letter', getHtml: () => '' }
    ];
  }

  private applyLetterTemplatesFromApi(rows: LetterTemplateDto[]): void {
    if (rows?.length) {
      this.letterTemplates = [...rows]
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        .map((r) => {
          const code = r.code;
          const body = r.bodyHtml ?? '';
          return {
            key: code,
            nameAr: r.nameAr,
            nameEn: r.nameEn,
            getHtml: () => {
              if (code === 'no-letter' || !body.trim()) {
                return '';
              }
              return this.buildBaseTemplate(body);
            }
          };
        });
    } else {
      this.letterTemplates = this.buildLocalLetterTemplates();
    }
    const first = this.letterTemplates[0];
    if (first) {
      this.onTemplateChange(first.key);
    }
  }

  letterTemplateTitle(t: LetterTemplateItem): string {
    const lang = this.i18n.currentLang();
    if (t.nameAr != null && t.nameEn != null && t.nameAr !== '' && t.nameEn !== '') {
      return lang === 'en' ? t.nameEn : t.nameAr;
    }
    return this.i18n.instant(`createTx.letterTemplate.${t.key}`);
  }

  get toArray(): FormArray {
    return this.secondaryForm.get('to') as FormArray;
  }

  get ccArray(): FormArray {
    return this.secondaryForm.get('cc') as FormArray;
  }

  deptName(id: number): string {
    return this.deptLabels.get(id) ?? String(id);
  }

  addTo(_value: string): void {
    /* selection via dialog only */
  }

  addCc(_value: string): void {
    /* selection via dialog only */
  }

  removeTo(index: number): void {
    this.toArray.removeAt(index);
  }

  removeCc(index: number): void {
    this.ccArray.removeAt(index);
  }
  submit(): void {
    if (!this.basicForm.valid || !this.secondaryForm.valid || !this.letterForm.valid) {
      this.showNotification(this.i18n.instant('createTx.validation.required'), 'error');
      return;
    }

    const maxDays = Number(this.secondaryForm.value.maxDays);
    const due = new Date();
    due.setDate(due.getDate() + (Number.isFinite(maxDays) ? maxDays : 5));

    const toIds = (this.toArray.value ?? []) as number[];
    const ownerDepartmentId = toIds.length ? toIds[0] : undefined;

    this.isLoading = true;

    const att = this.attachments;
    const uploads$ =
      att.length === 0
        ? of([] as CorrespondenceAttachmentFormDto[])
        : forkJoin(
            att.map((a) =>
              this.attachmentApi.upload(a.file).pipe(
                map(
                  (up): CorrespondenceAttachmentFormDto => ({
                    displayName: (a.name || a.file.name).trim(),
                    storageKey: up.storageKey,
                    byteSize: up.byteSize,
                    mimeType: up.mimeType ?? undefined
                  })
                )
              )
            )
          );

    uploads$.subscribe({
      next: (flat) => {
        const wfUser = (this.secondaryForm.value.workflowFirstAssigneeUserId ?? '')
          .toString()
          .trim();
        const wfRole = (this.secondaryForm.value.workflowFirstCandidateGroup ?? '')
          .toString()
          .trim();

        const body: CorrespondenceCreateRequest = {
          correspondenceTypeCode: this.basicForm.value.type,
          priorityCode: this.basicForm.value.priority,
          confidentialityCode: this.basicForm.value.secrecy,
          classificationCode: this.basicForm.value.classification,
          subject: (this.basicForm.value.subject ?? '').trim(),
          description: (this.basicForm.value.description ?? '').trim() || null,
          bodyHtml: (this.letterForm.value.letterContent ?? '') || null,
          ownerDepartmentId: ownerDepartmentId ?? null,
          dueDate: due.toISOString(),
          attachments: flat.length ? flat : undefined,
          ...(wfUser ? { workflowFirstAssigneeUserId: wfUser } : {}),
          ...(wfRole ? { workflowFirstCandidateGroup: wfRole } : {})
        };

        this.transactionService.create(body).subscribe({
          next: (res) => {
            this.isLoading = false;
            this.transactionNumber = res.referenceNumber;
            this.lastCreatedCorrespondenceId = res.id;
            this.showNotification(this.i18n.instant('createTx.submit.success'), 'success');
            setTimeout(() => this.goToStep(5), 0);
            setTimeout(() => this.generateBarcode(), 0);
          },
          error: (error: { status?: number; error?: { message?: string }; userMessage?: string }) => {
            this.isLoading = false;
            const errorMessage =
              error.userMessage ??
              error.error?.message ??
              this.i18n.instant('createTx.submit.errorGeneric');
            this.showNotification(errorMessage, 'error');
          }
        });
      },
      error: () => {
        this.isLoading = false;
        this.showNotification(this.i18n.instant('createTx.submit.errorGeneric'), 'error');
      }
    });
  }

  resetForms(): void {
    this.basicForm.reset();
    this.secondaryForm.reset({
      maxDays: 5,
      workflowFirstAssigneeUserId: '',
      workflowFirstCandidateGroup: ''
    });
    if (this.letterTemplates[0]) {
      this.onTemplateChange(this.letterTemplates[0].key);
    } else {
      this.letterForm.reset({
        letterContent: this.defaultTemplate()
      });
    }
    this.toArray.clear();
    this.ccArray.clear();
    this.attachments = [];
    this.transactionNumber = '';
    this.lastCreatedCorrespondenceId = null;
    this.currentStep = 1;
  }

  viewCreated(): void {
    if (this.lastCreatedCorrespondenceId) {
      this.router.navigate(['/transactions', this.lastCreatedCorrespondenceId]);
    }
  }

  createAnother(): void {
    this.resetForms();
  }

  finishWizard(): void {
    this.router.navigate(['/dashboard']);
  }

  showNotification(message: string, type: 'success' | 'error' | 'warning'): void {
    const panelClass = type === 'success' ? 'success-snackbar' :
      type === 'error' ? 'error-snackbar' : 'warning-snackbar';

    this.snackBar.open(message, this.i18n.instant('common.close'), {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
      panelClass: [panelClass]
    });
  }


  transactionTypes: { key: string }[] = [];

  secrecyLevels: { key: string }[] = [];

  priorityLevels: { key: string }[] = [];

  classificationLevels: { key: string }[] = [];


  openDepartmentDialog(type: 'to' | 'cc') {
    const currentValues = (type === 'to' ? this.toArray.value : this.ccArray.value) as number[];

    const dialogRef = this.dialog.open(DepartmentTreeDialogComponent, {
      width: '800px',
      data: currentValues ?? []
    });

    dialogRef.afterClosed().subscribe((result: number[] | undefined) => {
      if (!result || !result.length) return;

      this.zone.run(() => {
        const unique = [...new Set(result)];

        if (type === 'to') {
          this.toArray.clear();
          unique.forEach((id) => this.toArray.push(this.fb.control(id, { nonNullable: true })));
        } else {
          this.ccArray.clear();
          unique.forEach((id) => this.ccArray.push(this.fb.control(id, { nonNullable: true })));
        }
        this.cdr.markForCheck();
      });
    });
  }



  trackByIndex(index: number) {
    return index;
  }

  get hasTo(): boolean {
    return this.toArray.length > 0;
  }

  get hasCc(): boolean {
    return this.ccArray.length > 0;
  }


  // File attachment handling

  onFileSelected(event: any) {

    const files: FileList = event.target.files;

    if (!files) return;

    for (let i = 0; i < files.length; i++) {

      const file = files[i];

      this.attachments.push({
        file: file,
        name: file.name,
        description: ''
      });

    }

  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  onFileDrop(event: DragEvent) {

    event.preventDefault();

    if (!event.dataTransfer?.files) return;

    const files = event.dataTransfer.files;

    for (let i = 0; i < files.length; i++) {

      const file = files[i];

      this.attachments.push({
        file: file,
        name: file.name,
        description: ''
      });

    }

  }

  removeAttachment(index: number) {
    this.attachments.splice(index, 1);
  }

  formatFileSize(bytes: number): string {

    if (bytes === 0) return '0 KB';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];

  }

}
