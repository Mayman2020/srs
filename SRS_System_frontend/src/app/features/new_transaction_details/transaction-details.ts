import { ChangeDetectorRef, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, map, take, takeUntil } from 'rxjs/operators';

import {
  AttachmentIndexEntryDto,
  CorrespondenceCommentDetailDto,
  CorrespondenceDetailResponse,
  CorrespondenceNonarchivedItemDto,
  WorkflowActionAvailableDto,
  WorkflowHistoryEntryDto
} from '../../core/api/api-types';
import { CorrespondenceApiService } from '../../core/api/correspondence-api.service';
import { TransactionService } from '../../services/transaction.service';
import { PlatformWorkflowApiService } from '../../core/api/platform-workflow-api.service';
import { AttachmentApiService } from '../../core/api/attachment-api.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { CapabilitiesService } from '../../core/auth/capabilities.service';
import { AuthApiService } from '../../core/api/auth-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { HttpErrorResponse } from '@angular/common/http';

import { MatTabsModule } from '@angular/material/tabs';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { EditorModule } from '@tinymce/tinymce-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { VisualWorkflowDialogComponent } from '../visual-workflow-dialog/visual-workflow-dialog.component';
import { TextInputDialogComponent, TextInputDialogData } from '../../shared/dialogs/text-input-dialog.component';
import { ConfirmDialogComponent } from '../../shared/dialogs/confirm-dialog.component';
import {
  SendMailDialogComponent,
  SendMailDialogData,
} from '../../shared/dialogs/send-mail-dialog.component';
import { NotificationService } from '../../core/services/notification.service';



// ══════════════════════════════════════════════
// INTERFACES
// ══════════════════════════════════════════════

export interface TimelineStep {
  action: string;
  note: string;
  user: string;
  date: Date | string | null;
}

export interface Attachment {
  id: number;
  name: string;
  type: 'PDF' | 'Excel' | 'Word' | 'Image' | string;
  secrecy: string;
  size: string;
  date: string;
  url: string;
}

export interface TransactionNote {
  id: number;
  author: string;
  text: string;
  date: Date | string;
  typeClass: 'success' | 'info' | 'warning' | 'danger';
}

export interface RelatedTransaction {
  id: string;
  subject: string;
  referenceNumber?: string;
  created: Date | string | null;
  /** Link kind or label (displayed with {@link StatusBadgeComponent} `plain`). */
  status: string;
}

/** Maps file name / MIME to `attachment_content_type.code` (seeded in Flyway consolidated baseline). */
function guessAttachmentContentTypeCode(fileName: string, mimeType: string | undefined): string {
  const lower = fileName.toLowerCase();
  if (lower.endsWith('.pdf')) return 'PDF';
  if (lower.endsWith('.docx')) return 'DOCX';
  if (lower.endsWith('.doc')) return 'DOCX';
  if (lower.endsWith('.xlsx') || lower.endsWith('.xls')) return 'XLSX';
  if (lower.endsWith('.pptx') || lower.endsWith('.ppt')) return 'PPTX';
  if (/\.(jpe?g|png|tiff?|gif|webp|bmp)$/i.test(lower)) return 'IMAGE';
  if (lower.endsWith('.msg')) return 'MSG';
  const m = (mimeType ?? '').toLowerCase();
  if (m.includes('pdf')) return 'PDF';
  if (m.includes('word') || m.includes('msword')) return 'DOCX';
  if (m.includes('sheet') || m.includes('excel')) return 'XLSX';
  if (m.includes('presentation') || m.includes('powerpoint')) return 'PPTX';
  if (m.startsWith('image/')) return 'IMAGE';
  return '';
}

export interface Transaction {
  id: string;
  referenceNumber: string;
  subject: string;
  type: string;
  created: Date | string;
  dueDate: Date | string;
  secrecy: string;
  from: string;
  to: string;
  referredFrom?: string;
  status: string;
  /** Raw DB `correspondence_status.ui_variant` for `app-status-badge`. */
  statusUiVariant: string | null;
  maxDays: number;
  remainingDays: number;
  priority?: string;
  priorityClass?: 'low' | 'normal' | 'high' | 'urgent';
  priorityPercent?: number;
  currentHandler: string;
  timeline: TimelineStep[];
  attachments: Attachment[];
  notes: TransactionNote[];
  /** Camunda task decisions (labels from API). */
  workflowActions?: WorkflowActionAvailableDto[];
  /** Server-driven: show cancel action in toolbar. */
  cancelAllowed?: boolean;
}

// ══════════════════════════════════════════════
// COMPONENT
// ══════════════════════════════════════════════

@Component({
  selector: 'app-transaction-details',
  templateUrl: './transaction-details.html',
  styleUrls: ['./transaction-details.scss'],
  imports: [
    CommonModule,
    FormsModule,
    EditorModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatIconModule,
    MatDialogModule,
    MatButtonModule,
    LatinDigitsPipe,
    TranslatePipe,
    StatusBadgeComponent,
  ],
  standalone: true,
})
export class TransactionDetailsComponent implements OnInit, OnDestroy {

  /** Permission checks for toolbar actions (codes from {@code GET /api/v1/me/capabilities}). */
  readonly cap = inject(CapabilitiesService);

  @ViewChild('attachmentInput') attachmentInput?: ElementRef<HTMLInputElement>;

  // ── Data ────────────────────────────────────
  transaction!: Transaction;
  relatedTransactions: RelatedTransaction[] = [];
  nonarchivedItems: CorrespondenceNonarchivedItemDto[] = [];
  /** Backend attachment id → index rows */
  indexEntriesByAttachmentId: Record<number, AttachmentIndexEntryDto[]> = {};
  correspondenceUuid = '';

  // ── UI State ────────────────────────────────
  activeIndex = 2;
  newNote = '';
  canRefer = true;
  canAddNote = false;
  isOverdue = false;
  activeTab = "details" ;

  // ── Editor ──────────────────────────────────
  form!: FormGroup;

  editorInit = {
    height: 300,
    menubar: false,
    directionality: 'rtl',
    language: 'ar',
    plugins: ['lists', 'link', 'image', 'table', 'wordcount'],
    toolbar:
      'undo redo | bold italic underline | ' +
      'alignright aligncenter alignleft | ' +
      'bullist numlist | link image table',
    content_style:
      "body { font-family: 'Cairo', sans-serif; font-size: 14px; direction: rtl; }",
  };

  // ── Private ─────────────────────────────────
  private destroy$ = new Subject<void>();

  // ── Constructor ─────────────────────────────
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private transactionService: TransactionService,
    private platformWorkflow: PlatformWorkflowApiService,
    private attachmentApi: AttachmentApiService,
    private correspondenceApi: CorrespondenceApiService,
    private tokens: AuthTokenService,
    private authApi: AuthApiService,
    private i18n: I18nService,
    private notification: NotificationService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  // ══════════════════════════════════════════════
  // LIFECYCLE
  // ══════════════════════════════════════════════

  ngOnInit(): void {
    this.buildForm();
    this.loadTransaction();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ══════════════════════════════════════════════
  // GETTERS
  // ══════════════════════════════════════════════

  get completedSteps(): number {
    return this.transaction?.timeline?.length ?? 0;
  }

  get currentStepName(): string {
    if (!this.transaction?.timeline?.length) return '—';
    return this.transaction.timeline[this.activeIndex]?.action ?? '—';
  }

  // ══════════════════════════════════════════════
  // INIT HELPERS
  // ══════════════════════════════════════════════

  private buildForm(): void {
    this.form = this.fb.group({ letterContent: [''] });
  }

  private loadTransaction(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }

    this.correspondenceUuid = id;

    forkJoin({
      d: this.transactionService.getDetail(id),
      h: this.transactionService.getWorkflowHistory(id).pipe(
        catchError((err: unknown) => {
          console.error('[TransactionDetails] workflow history request failed', err);
          return of([] as WorkflowHistoryEntryDto[]);
        })
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ d, h }) => {
          this.transaction = this.mapDetail(d, h);
          const steps = this.transaction.timeline.length;
          this.activeIndex = steps > 0 ? steps - 1 : 0;
          this.checkOverdue();
          this.canAddNote = true;
          const draft = (d.replyDraftHtml ?? '').trim();
          this.form.patchValue({ letterContent: draft });
          this.cdr.detectChanges();
          this.loadGuideData();
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          console.error('[TransactionDetails] load correspondence failed', err);
          this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
          this.canAddNote = false;
          this.transaction = {
            id,
            referenceNumber: '—',
            subject: this.i18n.instant('errors.generic'),
            type: '—',
            created: new Date(),
            dueDate: new Date(),
            secrecy: '—',
            from: '—',
            to: '—',
            status: '—',
            statusUiVariant: null,
            maxDays: 0,
            remainingDays: 0,
            priority: '—',
            priorityClass: 'normal',
            priorityPercent: 0,
            currentHandler: '—',
            timeline: [],
            attachments: [],
            notes: [],
            cancelAllowed: false,
          };
          this.cdr.detectChanges();
        },
      });
  }

  private mapDetail(d: CorrespondenceDetailResponse, h: WorkflowHistoryEntryDto[]): Transaction {
    const isAr = this.i18n.currentLang() !== 'en';

    /** Return the name of a lookup item in the current UI language, falling back to the other language then the code. */
    const labelOf = (lookup: { nameAr: string; nameEn: string; code: string } | null): string =>
      ((isAr ? lookup?.nameAr : lookup?.nameEn) ?? (isAr ? lookup?.nameEn : lookup?.nameAr) ?? lookup?.code ?? '—');

    /** Return the name of an org/dept in the current UI language, falling back to the other language. */
    const nameOf = (item: { nameAr: string; nameEn: string } | null): string =>
      ((isAr ? item?.nameAr : item?.nameEn) ?? (isAr ? item?.nameEn : item?.nameAr) ?? '—');

    const created = d.createdAt ? new Date(d.createdAt) : new Date();
    const dueDate = d.dueDate ? new Date(d.dueDate) : new Date(created.getTime() + 5 * 86_400_000);
    const maxDays = Math.max(
      1,
      Math.ceil((dueDate.getTime() - created.getTime()) / 86_400_000)
    );
    const timelineDto =
      d.timeline && d.timeline.length > 0
        ? this.transactionService.detailTimelineToSteps(d.timeline)
        : this.transactionService.historyToTimeline(h);
    const steps = timelineDto.map((s) => ({
      action: s.action,
      note: s.note ?? '',
      user: s.user,
      date: s.date,
    }));

    const secrecyLabel = labelOf(d.confidentiality);

    return {
      id: d.id,
      referenceNumber: d.referenceNumber ?? d.id,
      subject: d.subject ?? '—',
      type: labelOf(d.correspondenceType),
      created,
      dueDate,
      secrecy: secrecyLabel,
      from: nameOf(d.senderOrganization),
      to: nameOf(d.recipientOrganization),
      status: d.correspondenceStatus?.code ?? '—',
      statusUiVariant: d.correspondenceStatus?.uiVariant ?? null,
      maxDays,
      remainingDays: Math.max(0, Math.ceil((dueDate.getTime() - Date.now()) / 86_400_000)),
      priority: labelOf(d.priority),
      priorityClass: 'normal',
      priorityPercent: 40,
      currentHandler: d.ownerDepartment
        ? ((isAr ? d.ownerDepartment.nameAr : d.ownerDepartment.nameEn) ?? d.ownerDepartment.code ?? '—')
        : '—',
      timeline: steps,
      attachments: (d.attachments ?? []).map((a) => ({
        id: a.id,
        name: a.displayName,
        type: a.contentType?.code ?? 'FILE',
        secrecy: secrecyLabel,
        size: this.formatBytes(
          (a.versions ?? []).reduce((m, v) => Math.max(m, v.byteSize), 0)
        ),
        date: (a.versions?.[0]?.createdAt ?? d.updatedAt ?? '').toString().substring(0, 10),
        url: this.attachmentApi.downloadUrl(a.id),
      })),
      notes: this.notesFromComments(d.comments ?? []),
      workflowActions: d.availableWorkflowActions ?? [],
      cancelAllowed: d.cancelAllowed !== false,
    };
  }

  private formatBytes(n: number): string {
    if (!n) {
      return '0 KB';
    }
    const k = 1024;
    const i = Math.floor(Math.log(n) / Math.log(k));
    return `${parseFloat((n / Math.pow(k, i)).toFixed(2))} ${['Bytes', 'KB', 'MB', 'GB'][i]}`;
  }

  private notesFromComments(rows: CorrespondenceCommentDetailDto[]): TransactionNote[] {
    return rows.map((c) => ({
      id: c.id,
      author:
        c.author?.fullNameAr?.trim() ||
        c.author?.fullNameEn?.trim() ||
        c.author?.username ||
        '—',
      text: c.body,
      date: new Date(c.createdAt),
      typeClass: 'info',
    }));
  }

  private loadGuideData(): void {
    const id = this.correspondenceUuid;
    if (!id) {
      return;
    }
    forkJoin({
      links: this.correspondenceApi.listLinks(id).pipe(catchError(() => of([]))),
      na: this.correspondenceApi.listNonarchived(id).pipe(catchError(() => of([])))
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ links, na }) => {
          this.relatedTransactions = (links ?? []).map((l) => ({
            id: l.linkedCorrespondenceId,
            subject: l.linkedSubject,
            referenceNumber: l.linkedReferenceNumber,
            created: null,
            status: l.linkKind
          }));
          this.nonarchivedItems = na ?? [];
          this.loadAttachmentIndexes();
        }
      });
  }

  private loadAttachmentIndexes(): void {
    this.indexEntriesByAttachmentId = {};
    const atts = this.transaction?.attachments ?? [];
    if (!atts.length) {
      return;
    }
    forkJoin(
      atts.map((att) =>
        this.correspondenceApi.listAttachmentIndexEntries(att.id).pipe(
          catchError(() => of([] as AttachmentIndexEntryDto[])),
          map((rows) => ({ id: att.id, rows }))
        )
      )
    )
      .pipe(takeUntil(this.destroy$), take(1))
      .subscribe((results) => {
        for (const r of results) {
          this.indexEntriesByAttachmentId[r.id] = r.rows;
        }
      });
  }

  private checkOverdue(): void {
    if (this.transaction?.dueDate) {
      this.isOverdue = new Date(this.transaction.dueDate) < new Date();
    }
  }

  // ══════════════════════════════════════════════
  // WORKFLOW HELPERS
  // ══════════════════════════════════════════════

  trackByStep(index: number, _step: TimelineStep): number {
    return index;
  }

  getAvatarColor(name: string): string {
    const colors = ['#1a6b3a', '#3b82f6', '#f59e0b', '#7c3aed', '#ef4444', '#0891b2'];
    if (!name) return colors[0];
    return colors[name.charCodeAt(0) % colors.length];
  }

  // ══════════════════════════════════════════════
  // ACTION HANDLERS
  // ══════════════════════════════════════════════

  private toast(msg: string, type: 'success' | 'error' | 'warning' | 'info' = 'info'): void {
    if (type === 'success') {
      this.notification.successRaw(msg);
      return;
    }
    if (type === 'warning') {
      this.notification.warningRaw(msg);
      return;
    }
    if (type === 'error') {
      this.notification.errorRaw(msg);
      return;
    }
    this.notification.infoRaw(msg);
  }

  /** Display label from DB for current language. */
  workflowActionLabel(a: WorkflowActionAvailableDto): string {
    return this.i18n.currentLang() === 'en' ? a.nameEn : a.nameAr;
  }

  /** Emphasized action in footer: first row as returned by API (ordered by `sortOrder`). */
  primaryWorkflowAction(): WorkflowActionAvailableDto | undefined {
    const w = this.transaction?.workflowActions;
    if (!w?.length) {
      return undefined;
    }
    return w[0];
  }

  /**
   * Maps DB `ui_variant` to existing action-bar CSS classes (no per-action-code branching).
   */
  workflowActionUiClass(uiVariant: string | null | undefined): string {
    const v = (uiVariant ?? 'secondary').toLowerCase();
    switch (v) {
      case 'primary':
        return 'blue';
      case 'danger':
        return 'red-outline';
      case 'warning':
        return 'orange';
      case 'success':
        return 'green';
      case 'secondary':
      default:
        return 'gray';
    }
  }

  onWorkflowAction(a: WorkflowActionAvailableDto): void {
    if (a.requiresComment) {
      const ref = this.dialog.open(TextInputDialogComponent, {
        width: 'min(480px, 94vw)',
        autoFocus: 'dialog',
        data: {
          dialogTitle: this.workflowActionLabel(a),
          labelKey: 'transactionDetails.workflowCommentPrompt',
          confirmKey: 'common.apply',
          required: true,
          multiline: true,
        } satisfies TextInputDialogData,
      });
      ref
        .afterClosed()
        .pipe(take(1))
        .subscribe((comment) => {
          if (comment === undefined) {
            return;
          }
          if (!String(comment).trim()) {
            this.toast(this.i18n.instant('transactionDetails.workflowCommentRequired'), 'warning');
            return;
          }
          this.runWorkflowAction(a.code, String(comment).trim());
        });
      return;
    }
    this.runWorkflowAction(a.code);
  }

  private runWorkflowAction(action: string, comment?: string | null): void {
    if (!this.correspondenceUuid) {
      return;
    }
    this.transactionService
      .workflowAction(this.correspondenceUuid, { action, comment })
      .subscribe({
        next: () => this.loadTransaction(),
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
        },
      });
  }

  /** Delegates the current user’s Camunda task to another user (UUID). */
  delegateWorkflow(): void {
    if (!this.correspondenceUuid) {
      return;
    }
    this.dialog
      .open(TextInputDialogComponent, {
        width: 'min(440px, 94vw)',
        autoFocus: 'dialog',
        data: {
          titleKey: 'transactionDetails.workflowDelegate',
          labelKey: 'transactionDetails.workflowDelegatePrompt',
          confirmKey: 'common.apply',
          required: true,
          multiline: false,
        } satisfies TextInputDialogData,
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((delegateeUserId) => {
        if (!delegateeUserId?.trim()) {
          return;
        }
        const id = this.correspondenceUuid;
        if (!id) {
          return;
        }
        this.platformWorkflow
          .delegateCorrespondence(id, {
            delegateeUserId: delegateeUserId.trim(),
          })
          .subscribe({
            next: () => {
              this.toast(this.i18n.instant('transactionDetails.workflowDelegateSuccess'), 'success');
              this.loadTransaction();
            },
            error: (err: HttpErrorResponse & { userMessage?: string }) => {
              this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
            },
          });
      });
  }

  openSendMailDialog(): void {
    if (!this.transaction) {
      return;
    }
    const defaultSubject = `[${this.transaction.referenceNumber}] ${this.transaction.subject}`;
    const defaultBody = this.i18n.instant('transactionDetails.sendMailBodyDefault', {
      ref: this.transaction.referenceNumber,
      url: typeof window !== 'undefined' ? window.location.href : '',
    });
    this.dialog
      .open(SendMailDialogComponent, {
        width: 'min(480px, 94vw)',
        data: {
          defaultSubject,
          defaultBody,
        } satisfies SendMailDialogData,
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((ok) => {
        if (ok) {
          this.toast(this.i18n.instant('transactionDetails.sendMailSuccess'), 'success');
        }
      });
  }

  openCancelDialog(): void {
    if (!this.correspondenceUuid) {
      return;
    }
    const cid = this.correspondenceUuid;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: 'min(400px, 92vw)',
        data: {
          titleKey: 'transactionDetails.cancelDialogTitle',
          messageKey: 'transactionDetails.cancelDialogMessage',
          confirmKey: 'transactionDetails.cancelConfirm',
          cancelKey: 'common.close',
          warn: true,
        },
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.transactionService.cancelCorrespondence(cid).subscribe({
          next: () => {
            this.toast(this.i18n.instant('transactionDetails.cancelSuccess'), 'success');
            this.router.navigate(['/transactions']);
          },
          error: (err: HttpErrorResponse & { userMessage?: string }) => {
            this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
          },
        });
      });
  }

  showImageTransferPending(): void {
    this.openAttachmentFilePicker();
  }

  printTransaction(): void {
    window.print();
  }

  shareTransaction(): void {
    navigator.clipboard?.writeText(window.location.href);
  }

  // ══════════════════════════════════════════════
  // EDITOR
  // ══════════════════════════════════════════════

  saveDraft(): void {
    if (!this.correspondenceUuid) {
      return;
    }
    const html = (this.form.get('letterContent')?.value ?? '') as string;
    this.transactionService.saveReplyDraft(this.correspondenceUuid, html).subscribe({
      next: () => this.toast(this.i18n.instant('transactionDetails.saveDraftSuccess'), 'success'),
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  sendReply(): void {
    if (!this.correspondenceUuid) {
      return;
    }
    const html = ((this.form.get('letterContent')?.value ?? '') as string).trim();
    if (!html) {
      this.toast(this.i18n.instant('transactionDetails.sendReplyEmpty'), 'warning');
      return;
    }
    this.transactionService.sendCorrespondenceReply(this.correspondenceUuid, html).subscribe({
      next: () => {
        this.toast(this.i18n.instant('transactionDetails.sendReplySuccess'), 'success');
        this.form.patchValue({ letterContent: '' });
        this.loadTransaction();
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  // ══════════════════════════════════════════════
  // ATTACHMENTS
  // ══════════════════════════════════════════════

  addAttachment(): void {
    this.openAttachmentFilePicker();
  }

  openAttachmentFilePicker(): void {
    const el = this.attachmentInput?.nativeElement;
    if (el) {
      el.value = '';
      el.click();
    }
  }

  onAttachmentPicked(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file || !this.correspondenceUuid) {
      return;
    }
    this.attachmentApi.upload(file).subscribe({
      next: (res) => {
        const contentTypeCode = guessAttachmentContentTypeCode(file.name, res.mimeType);
        this.transactionService
          .addCorrespondenceAttachment(this.correspondenceUuid!, {
            displayName: file.name,
            storageKey: res.storageKey,
            byteSize: res.byteSize,
            mimeType: res.mimeType,
            ...(contentTypeCode ? { contentTypeCode } : {}),
          })
          .subscribe({
            next: () => {
              this.toast(this.i18n.instant('transactionDetails.addAttachmentSuccess'), 'success');
              this.loadTransaction();
            },
            error: (err: HttpErrorResponse & { userMessage?: string }) => {
              this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
            },
          });
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  previewAttachment(att: Attachment): void {
    this.downloadWithAuth(att.id, att.name, true);
  }

  downloadAttachment(att: Attachment): void {
    this.downloadWithAuth(att.id, att.name, false);
  }

  private downloadWithAuth(attachmentId: number, filename: string, openInTab: boolean): void {
    const token = this.tokens.getToken();
    if (!token) {
      this.toast(this.i18n.instant('transactionDetails.downloadNoSession'), 'warning');
      this.authApi.logout();
      void this.router.navigate(['/login']);
      return;
    }
    void fetch(this.attachmentApi.downloadUrl(attachmentId), {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (r.status === 401 || r.status === 403) {
          this.ngZone.run(() => {
            this.toast(this.i18n.instant('transactionDetails.downloadNoSession'), 'warning');
            this.authApi.logout();
            void this.router.navigate(['/login']);
          });
          throw new Error('session invalid');
        }
        if (!r.ok) {
          throw new Error('download failed');
        }
        return r.blob();
      })
      .then((blob) => {
        this.ngZone.run(() => {
          const url = URL.createObjectURL(blob);
          if (openInTab) {
            window.open(url, '_blank', 'noopener');
          } else {
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            a.click();
          }
          setTimeout(() => URL.revokeObjectURL(url), 60_000);
        });
      })
      .catch((e: unknown) => {
        if (e instanceof Error && e.message === 'session invalid') {
          return;
        }
        console.error('[TransactionDetails] attachment download failed', e);
        this.ngZone.run(() => this.toast(this.i18n.instant('errors.generic'), 'error'));
      });
  }

  deleteAttachment(att: Attachment): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        width: 'min(400px, 92vw)',
        data: {
          titleKey: 'transactionDetails.deleteAttachmentTitle',
          messageKey: 'transactionDetails.deleteAttachmentConfirm',
          confirmKey: 'transactionDetails.deleteAttachmentConfirmBtn',
          cancelKey: 'common.close',
          warn: true,
        },
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.attachmentApi.delete(att.id).subscribe({
          next: () => {
            this.toast(this.i18n.instant('transactionDetails.deleteAttachmentSuccess'), 'success');
            this.loadTransaction();
          },
          error: (err: HttpErrorResponse & { userMessage?: string }) => {
            this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
          },
        });
      });
  }

  // ══════════════════════════════════════════════
  // NOTES
  // ══════════════════════════════════════════════

  submitNote(): void {
    const text = this.newNote?.trim();
    if (!text || !this.correspondenceUuid) {
      return;
    }
    this.transactionService.addComment(this.correspondenceUuid, text).subscribe({
      next: () => {
        this.newNote = '';
        this.loadTransaction();
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  // ══════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════

  navigateToTransaction(id: string): void {
    this.router.navigate(['/transactions', id]);
  }


  openVisualTracking(): void {
    if (!this.transaction) {
      return;
    }

    this.dialog.open(VisualWorkflowDialogComponent, {
      data: {
        transaction: this.transaction,
        activeIndex: this.activeIndex,
      },
      width: 'min(1100px, 96vw)',
      maxWidth: '96vw',
      height: 'min(85vh, 900px)',
      panelClass: 'visual-tracking-dialog',
      autoFocus: false,
    });
  }
}
