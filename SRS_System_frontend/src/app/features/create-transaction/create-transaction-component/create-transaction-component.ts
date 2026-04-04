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
import { TransactionPayload, TransactionService } from '../../../services/transaction.service';
import { LookupService } from '../../../core/api/lookup.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { LookupTranslatePipe } from '../../../core/i18n/lookup-translate.pipe';
import { GenericSelectComponent } from '../../../component/generic-select/generic-select.component';
import { EditorModule } from '@tinymce/tinymce-angular';
import { DepartmentTreeDialogComponent } from '../department-tree-dialog/department-tree-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import JsBarcode from 'jsbarcode';


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
  transactionNumber = 'TRX-2026-000123';

  ngOnInit(): void {
    this.letterForm.patchValue({
      letterContent: this.defaultTemplate()
    });

    this.lookupService.getBundle().subscribe({
      next: (b) => {
        this.transactionTypes = b.correspondenceTypes.map((t) => ({ key: t.code }));
        this.secrecyLevels = b.confidentialities.map((c) => ({ key: c.code }));
      },
      error: () => {
        this.transactionTypes = [];
        this.secrecyLevels = [];
      }
    });
  }

  generateBarcode() {

    setTimeout(() => {

      if (this.barcode?.nativeElement) {

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
  nextStep() {
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
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    private lookupService: LookupService,
    private i18n: I18nService
  ) {

    // Step 1
    this.basicForm = this.fb.group({
      type: ['', Validators.required],
      secrecy: ['', Validators.required],
      subject: ['', [Validators.required, Validators.minLength(5)]],
      description: ['']
    });

    // Step 2
    this.secondaryForm = this.fb.group({
      from: ['', Validators.required],
      to: this.fb.array([], Validators.required),
      cc: this.fb.array([]),
      maxDays: [5, [Validators.required, Validators.min(1), Validators.max(30)]]
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
    placeholder: 'اكتب محتوى الخطاب هنا...',
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

    const template = this.letterTemplates.find(t => t.key === key);
    if (!template) return;

    this.letterForm.patchValue({
      letterContent: template.getHtml()
    });
  }
  /* ================================
     LETTER TEMPLATES SYSTEM (Premium)
  ================================ */

  buildBaseTemplate(bodyContent: string): string {
    const today = new Date().toLocaleDateString('ar-EG', {
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
      اسم الجهة
    </h1>

    <div style="color:#6b7280;font-size:16px;">
      الإدارة العامة للاتصالات الإدارية
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
      <strong style="color:#0B6E4F">التاريخ:</strong> ${today}
    </div>
    <div>
      <strong style="color:#0B6E4F">رقم المعاملة:</strong> يُملأ تلقائياً
    </div>
  </div>

  ${bodyContent}

  <div style="margin-top:120px;">
    <div style="font-weight:700;color:#0B6E4F;margin-bottom:14px;">
      مدير الإدارة
    </div>

    <div style="
      width:260px;
      border-bottom:2px solid #9ca3af;
      margin-top:30px;
    "></div>

    <div style="margin-top:10px;color:#6b7280;">
      التوقيع
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
    <p><span style="font-weight:600;color:#0B6E4F">إلى:</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">الموضوع:</span> .......................................................</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      السلام عليكم ورحمة الله وبركاته، وبعد:
    </p>

    <p style="line-height:2.2;text-align:justify;">
      نحيطكم علماً بأنه تم استلام خطابكم بشأن
      <strong>[الموضوع]</strong>،
      ونفيدكم بما يلي:
    </p>

    <ol style="margin: 20px 0; padding-right: 40px; line-height: 2.2;">
      <li>.......................................................</li>
      <li>.......................................................</li>
      <li>.......................................................</li>
    </ol>

    <p>نشكر لكم حُسن تعاونكم.</p>
  `);
  }

  /* ================================
     REMINDER
  ================================ */
  reminderTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">إلى:</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">الموضوع:</span> تذكير بخصوص معاملة</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      السلام عليكم ورحمة الله وبركاته، وبعد:
    </p>

    <p style="line-height:2.2;text-align:justify;">
      بالإشارة إلى المعاملة رقم <strong>[رقم المعاملة]</strong>،
      نود تذكيركم بضرورة الإفادة في أقرب وقت ممكن.
    </p>

    <p>شاكرين تعاونكم.</p>
  `);
  }

  /* ================================
     APPROVAL
  ================================ */
  approvalTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">إلى:</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">الموضوع:</span> إفادة بالموافقة</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      السلام عليكم ورحمة الله وبركاته، وبعد:
    </p>

    <p style="line-height:2.2;text-align:justify;">
      نفيدكم بأنه تمت الموافقة على الطلب المتعلق بـ
      <strong>[الموضوع]</strong>.
      يرجى استكمال الإجراءات اللازمة.
    </p>
  `);
  }

  /* ================================
     REJECTION
  ================================ */
  rejectionTemplate(): string {
    return this.buildBaseTemplate(`
    <p><span style="font-weight:600;color:#0B6E4F">إلى:</span> .......................................................</p>
    <p><span style="font-weight:600;color:#0B6E4F">الموضوع:</span> اعتذار عن عدم الموافقة</p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="text-align:center;font-weight:600;color:#0B6E4F;">
      السلام عليكم ورحمة الله وبركاته، وبعد:
    </p>

    <p style="line-height:2.2;text-align:justify;">
      نعتذر عن عدم إمكانية الموافقة على الطلب نظراً لـ
      <strong>[سبب الرفض]</strong>.
    </p>
  `);
  }

  /* ================================
     ADMINISTRATIVE CIRCULAR
  ================================ */
  administrativeCircularTemplate(): string {
    return this.buildBaseTemplate(`
    <p style="text-align:center;font-weight:700;font-size:18px;color:#0B6E4F;">
      تعميم إداري
    </p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="line-height:2.2;text-align:justify;">
      تعلن الإدارة عن <strong>[موضوع التعميم]</strong>.
      يرجى من جميع الإدارات الالتزام بما ورد أعلاه.
    </p>
  `);
  }

  /* ================================
     MINISTERIAL CIRCULAR
  ================================ */
  ministerialCircularTemplate(): string {
    return this.buildBaseTemplate(`
    <p style="text-align:center;font-weight:700;font-size:18px;color:#0B6E4F;">
      تعميم وزاري
    </p>

    <hr style="margin:30px 0;border:1px solid #e5e7eb;">

    <p style="line-height:2.2;text-align:justify;">
      بناءً على التوجيهات الوزارية بشأن
      <strong>[موضوع التعميم]</strong>،
      يُعتمد هذا التعميم للعمل به في جميع الجهات.
    </p>
  `);
  }

  /* ================================
     TEMPLATES LIST
  ================================ */
  letterTemplates: { key: string; getHtml: () => string }[] = [
    { key: 'default', getHtml: () => this.defaultTemplate() },
    { key: 'reminder', getHtml: () => this.reminderTemplate() },
    { key: 'approval', getHtml: () => this.approvalTemplate() },
    { key: 'rejection', getHtml: () => this.rejectionTemplate() },
    { key: 'admin-circular', getHtml: () => this.administrativeCircularTemplate() },
    { key: 'ministerial-circular', getHtml: () => this.ministerialCircularTemplate() },
    { key: 'no-letter', getHtml: () => '' }
  ];

  letterTemplateTitle(key: string): string {
    return this.i18n.instant(`createTx.letterTemplate.${key}`);
  }

  get toArray(): FormArray {
    return this.secondaryForm.get('to') as FormArray;
  }

  get ccArray(): FormArray {
    return this.secondaryForm.get('cc') as FormArray;
  }

  addTo(value: string): void {
    if (!value?.trim()) return;
    this.toArray.push(this.fb.control(value.trim()));
  }

  addCc(value: string): void {
    if (!value?.trim()) return;
    this.ccArray.push(this.fb.control(value.trim()));
  }

  removeTo(index: number): void {
    this.toArray.removeAt(index);
  }

  removeCc(index: number): void {
    this.ccArray.removeAt(index);
  }
  submit(): void {
    if (!this.basicForm.valid || !this.secondaryForm.valid) {
      this.showNotification(this.i18n.instant('createTx.validation.required'), 'error');
      return;
    }

    const payload: TransactionPayload = {
      type: this.basicForm.value.type,
      secrecy: this.basicForm.value.secrecy,
      subject: this.basicForm.value.subject,
      description: this.basicForm.value.description,
      from: this.secondaryForm.value.from,
      to: this.toArray.value,
      cc: this.ccArray.value,
      maxDays: this.secondaryForm.value.maxDays,
      letterContent: this.letterForm.value.letterContent
    };

    console.log('Sending Payload:', payload);
    this.isLoading = true;

    this.transactionService.createTransaction(payload).subscribe({
      next: () => {
        this.isLoading = false;
        this.showNotification(this.i18n.instant('createTx.submit.success'), 'success');
        this.resetForms();
      },
      error: (error) => {
        this.isLoading = false;
        let errorMessage = this.i18n.instant('createTx.submit.errorGeneric');
        if (error.status === 501) {
          errorMessage =
            typeof error.error === 'string'
              ? error.error
              : this.i18n.instant('createTx.submit.notImplementedDetail');
          this.showNotification(errorMessage, 'warning');
          return;
        }
        if (error.status === 401) {
          errorMessage = this.i18n.instant('createTx.submit.unauthorized');
        } else if (error.status === 400) {
          errorMessage = error.error?.message || this.i18n.instant('createTx.submit.badRequest');
        } else if (error.status === 500) {
          errorMessage = this.i18n.instant('createTx.submit.serverError');
        }
        this.showNotification(errorMessage, 'error');
      }
    });
  }

  resetForms(): void {
    this.basicForm.reset();
    this.secondaryForm.reset({ maxDays: 5 });
    this.letterForm.reset({ letterContent: this.defaultTemplate() });
    this.toArray.clear();
    this.ccArray.clear();

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


  openDepartmentDialog(type: 'to' | 'cc') {
    const currentValues = type === 'to'
      ? this.toArray.value
      : this.ccArray.value;

    const dialogRef = this.dialog.open(DepartmentTreeDialogComponent, {
      width: '800px',
      data: currentValues
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result || !result.length) return;

      this.zone.run(() => {
        const unique = [...new Set<string>(result)];

        if (type === 'to') {
          this.toArray.clear();
          unique.forEach(v => this.toArray.push(this.fb.control(v)));
        } else {
          this.ccArray.clear();
          unique.forEach(v => this.ccArray.push(this.fb.control(v)));
        }
        this.cdr.detectChanges();
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
