import { ChangeDetectorRef, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, map, take, takeUntil } from 'rxjs/operators';

import {
  AttachmentIndexEntryDto,
  CorrespondenceCommentDetailDto,
  CorrespondenceDetailResponseDto,
  CorrespondenceNonarchivedItemDto,
  CorrespondenceReadReceiptDto,
  CorrespondenceReadStatusSummaryDto,
  AttachmentAccessLogDto,
  WorkflowActionAvailableDto,
  WorkflowHistoryEntryDto
} from '../../core/api/api-types';
import { CorrespondenceApiService } from '../../core/api/correspondence-api.service';
import { CorrespondenceReadTrackingApiService } from '../../core/api/correspondence-read-tracking-api.service';
import { AttachmentAccessLogApiService } from '../../core/api/attachment-access-log-api.service';
import { TransactionService } from '../../core/services/transaction.service';
import { PlatformWorkflowApiService } from '../../core/api/platform-workflow-api.service';
import { AttachmentApiService } from '../../core/api/attachment-api.service';
import { AttachmentDownloadApiService } from '../../core/api/attachment-download-api.service';
import { DocumentSignatureApiService } from '../../core/api/document-signature-api.service';
import type { DocumentSignatureDto } from '../../core/api/api-types';
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
  VerificationQrDialogComponent,
  VerificationQrDialogData,
} from './verification-qr-dialog.component';
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
  /** Slice 5 — true when the parent correspondence's confidentiality requires clearance. */
  classified?: boolean;
  /** Slice 5 — id of the current attachment_version (drives signature panel + verify links). */
  currentVersionId?: number | null;
  /** Slice 5 — true when the on-disk blob is encrypted (`encryption_algo` non-null). */
  encrypted?: boolean;
  /** Slice 5 — signatures on the current version (lazy-loaded when the panel opens). */
  signatures?: DocumentSignatureDto[];
}

export interface TransactionNote {
  id: number;
  author: string;
  text: string;
  date: Date | string;
  typeClass: 'success' | 'info' | 'warning' | 'danger';
}

export interface RelatedTransaction {
  linkId: number;
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
  accessLogEntries: AttachmentAccessLogDto[] = [];
  accessLogLoading = false;
  /** Backend attachment id → index rows */
  indexEntriesByAttachmentId: Record<number, AttachmentIndexEntryDto[]> = {};
  correspondenceUuid = '';

  /** Slice 1 — calling user's read receipt for this correspondence (`null` until tracked). */
  myReadReceipt: CorrespondenceReadReceiptDto | null = null;
  /** Slice 1 — server-driven flag; defaults to true if absent. */
  acknowledgementSupported = true;
  /** Slice 1 — cross-user read status, lazily fetched on demand. */
  readStatus: CorrespondenceReadStatusSummaryDto | null = null;
  readStatusLoading = false;
  readStatusPanelOpen = false;
  /** Optional comment captured before calling acknowledge. */
  ackCommentDraft = '';
  ackInProgress = false;
  /** Permission codes for Slice 1 panels (kept as literals, only ever compared to API codes). */
  readonly READ_STATUS_VIEW_PERMISSION = 'CORRESPONDENCE_READ_STATUS_VIEW';
  readonly ACCESS_LOG_VIEW_PERMISSION = 'ATTACHMENT_ACCESS_LOG_VIEW';
  readonly CORRESPONDENCE_UPDATE_PERMISSION = 'CORRESPONDENCE_UPDATE';

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
    private attachmentDownloadApi: AttachmentDownloadApiService,
    private signatureApi: DocumentSignatureApiService,
    private correspondenceApi: CorrespondenceApiService,
    private readTrackingApi: CorrespondenceReadTrackingApiService,
    private accessLogApi: AttachmentAccessLogApiService,
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
          this.myReadReceipt = d.myReadReceipt ?? null;
          this.acknowledgementSupported = d.acknowledgementSupported !== false;
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

  private mapDetail(d: CorrespondenceDetailResponseDto, h: WorkflowHistoryEntryDto[]): Transaction {
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
      attachments: (d.attachments ?? []).map((a) => {
        const current = a.versions?.find((v) => v.id === a.currentVersionId) ?? a.versions?.[0] ?? null;
        return {
          id: a.id,
          name: a.displayName,
          type: a.contentType?.code ?? 'FILE',
          secrecy: secrecyLabel,
          size: this.formatBytes(
            (a.versions ?? []).reduce((m, v) => Math.max(m, v.byteSize), 0)
          ),
          date: (a.versions?.[0]?.createdAt ?? d.updatedAt ?? '').toString().substring(0, 10),
          url: '',
          classified: d.confidentiality?.requiresClearance === true,
          currentVersionId: a.currentVersionId ?? current?.id ?? null,
          encrypted: !!current?.encryptionAlgo,
          signatures: undefined,
        };
      }),
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
            linkId: l.id,
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

  canEditGuide(): boolean {
    return this.cap.can(this.CORRESPONDENCE_UPDATE_PERMISSION);
  }

  canViewAccessLog(): boolean {
    return this.cap.can(this.ACCESS_LOG_VIEW_PERMISSION);
  }

  onTabChange(tab: string): void {
    this.activeTab = tab;
    if (tab === 'accesslog' && this.canViewAccessLog()) {
      this.loadAccessLog();
    }
  }

  private loadAccessLog(): void {
    const id = this.correspondenceUuid;
    if (!id) {
      return;
    }
    this.accessLogLoading = true;
    this.accessLogApi
      .forCorrespondence(id)
      .pipe(takeUntil(this.destroy$), take(1))
      .subscribe({
        next: (rows) => {
          this.accessLogEntries = rows ?? [];
          this.accessLogLoading = false;
        },
        error: () => {
          this.accessLogEntries = [];
          this.accessLogLoading = false;
        }
      });
  }

  addRelatedLink(): void {
    if (!this.canEditGuide() || !this.correspondenceUuid) {
      return;
    }
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(480px, 94vw)',
      autoFocus: 'dialog',
      data: {
        dialogTitle: this.i18n.instant('transactionDetails.addRelated'),
        labelKey: 'transactionDetails.linkedCorrespondenceId',
        confirmKey: 'common.apply',
        required: true,
        multiline: false,
      } satisfies TextInputDialogData
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe((linkedId) => {
        const id = String(linkedId ?? '').trim();
        if (!id) {
          return;
        }
        this.correspondenceApi
          .addLink(this.correspondenceUuid, { linkedCorrespondenceId: id, linkKind: 'RELATED' })
          .subscribe({
            next: () => {
              this.toast(this.i18n.instant('transactionDetails.relatedAdded'), 'success');
              this.loadGuideData();
            },
            error: () => this.toast(this.i18n.instant('errors.generic'), 'error')
          });
      });
  }

  deleteRelatedLink(rel: RelatedTransaction, event: Event): void {
    event.stopPropagation();
    if (!this.canEditGuide() || !this.correspondenceUuid) {
      return;
    }
    this.correspondenceApi.deleteLink(this.correspondenceUuid, rel.linkId).subscribe({
      next: () => {
        this.toast(this.i18n.instant('transactionDetails.relatedDeleted'), 'success');
        this.loadGuideData();
      },
      error: () => this.toast(this.i18n.instant('errors.generic'), 'error')
    });
  }

  addNonarchivedItem(): void {
    if (!this.canEditGuide() || !this.correspondenceUuid) {
      return;
    }
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(480px, 94vw)',
      autoFocus: 'dialog',
      data: {
        dialogTitle: this.i18n.instant('transactionDetails.addNonarchived'),
        labelKey: 'transactionDetails.colDescription',
        confirmKey: 'common.apply',
        required: true,
        multiline: true
      } satisfies TextInputDialogData
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe((description) => {
        const text = String(description ?? '').trim();
        if (!text) {
          return;
        }
        this.correspondenceApi
          .addNonarchived(this.correspondenceUuid, {
            itemType: 'OTHER',
            descriptionText: text,
            quantity: 1,
            sortOrder: this.nonarchivedItems.length
          })
          .subscribe({
            next: () => {
              this.toast(this.i18n.instant('transactionDetails.nonarchivedAdded'), 'success');
              this.loadGuideData();
            },
            error: () => this.toast(this.i18n.instant('errors.generic'), 'error')
          });
      });
  }

  deleteNonarchivedItem(item: CorrespondenceNonarchivedItemDto): void {
    if (!this.canEditGuide() || !this.correspondenceUuid) {
      return;
    }
    this.correspondenceApi.deleteNonarchived(this.correspondenceUuid, item.id).subscribe({
      next: () => {
        this.toast(this.i18n.instant('transactionDetails.nonarchivedDeleted'), 'success');
        this.loadGuideData();
      },
      error: () => this.toast(this.i18n.instant('errors.generic'), 'error')
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
    if (a.requiresSignature && this.workflowActionSignatureMissing(a)) {
      this.toast(this.i18n.instant('attachments.signRequiredTooltip'), 'warning');
      return;
    }
    if (a.requiresTargetUser) {
      this.promptReferAction(a);
      return;
    }
    if (a.requiresTargetDepartment) {
      this.promptForwardAction(a);
      return;
    }
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

  private runWorkflowAction(
    action: string,
    comment?: string | null,
    targetUserId?: string | null,
    targetDepartmentId?: number | null
  ): void {
    if (!this.correspondenceUuid) {
      return;
    }
    this.transactionService
      .workflowAction(this.correspondenceUuid, {
        action,
        comment,
        targetUserId,
        targetDepartmentId
      })
      .subscribe({
        next: () => this.loadTransaction(),
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
        },
      });
  }

  private promptReferAction(a: WorkflowActionAvailableDto): void {
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(480px, 94vw)',
      autoFocus: 'dialog',
      data: {
        dialogTitle: this.workflowActionLabel(a),
        labelKey: 'transactionDetails.workflowReferUserPrompt',
        confirmKey: 'common.apply',
        required: true,
        multiline: false,
      } satisfies TextInputDialogData,
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe((targetUserId) => {
        if (!targetUserId?.trim()) {
          return;
        }
        if (a.requiresComment) {
          this.promptCommentThenRun(a.code, targetUserId.trim());
          return;
        }
        this.runWorkflowAction(a.code, null, targetUserId.trim());
      });
  }

  private promptForwardAction(a: WorkflowActionAvailableDto): void {
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(480px, 94vw)',
      autoFocus: 'dialog',
      data: {
        dialogTitle: this.workflowActionLabel(a),
        labelKey: 'transactionDetails.workflowForwardDeptPrompt',
        confirmKey: 'common.apply',
        required: true,
        multiline: false,
      } satisfies TextInputDialogData,
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe((deptRaw) => {
        const deptId = Number(String(deptRaw ?? '').trim());
        if (!Number.isFinite(deptId) || deptId <= 0) {
          return;
        }
        if (a.requiresComment) {
          this.promptCommentThenRun(a.code, undefined, deptId);
          return;
        }
        this.runWorkflowAction(a.code, null, null, deptId);
      });
  }

  private promptCommentThenRun(
    action: string,
    targetUserId?: string,
    targetDepartmentId?: number
  ): void {
    const ref = this.dialog.open(TextInputDialogComponent, {
      width: 'min(480px, 94vw)',
      autoFocus: 'dialog',
      data: {
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
        if (!String(comment ?? '').trim()) {
          this.toast(this.i18n.instant('transactionDetails.workflowCommentRequired'), 'warning');
          return;
        }
        this.runWorkflowAction(action, String(comment).trim(), targetUserId, targetDepartmentId);
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
            this.router.navigate(['/correspondence']);
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
            ...(res.plaintextSha256 ? { plaintextSha256: res.plaintextSha256 } : {}),
            ...(res.encryptionAlgo ? { encryptionAlgo: res.encryptionAlgo } : {}),
            ...(res.encryptionKeyRef ? { encryptionKeyRef: res.encryptionKeyRef } : {}),
            ...(res.encryptionWrappedDekB64 ? { encryptionWrappedDekB64: res.encryptionWrappedDekB64 } : {}),
            ...(res.encryptionIvB64 ? { encryptionIvB64: res.encryptionIvB64 } : {}),
            ...(res.ciphertextSha256 ? { ciphertextSha256: res.ciphertextSha256 } : {}),
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

  openVerificationQr(att: Attachment): void {
    const data: VerificationQrDialogData = { attachmentId: att.id, fileLabel: att.name };
    this.dialog.open(VerificationQrDialogComponent, { width: '420px', data });
  }

  /**
   * Slice 5 — two-step signed-download flow.
   *
   *   1. `POST /attachments/{id}/download-intent` returns `{ token, expiresAt }`.
   *   2. `GET /attachments/download/{token}` streams the decrypted bytes (token is single-use,
   *      bound to the issuing user, and expires after `ac.attachment.download-token.ttl-seconds`).
   *
   * The legacy `GET /attachments/{id}/download` path returns {@code 410 Gone} (Slice 6); this UI
   * uses the intent + token pipeline only.
   */
  private downloadWithAuth(attachmentId: number, filename: string, openInTab: boolean): void {
    const jwt = this.tokens.getToken();
    if (!jwt) {
      this.toast(this.i18n.instant('transactionDetails.downloadNoSession'), 'warning');
      this.authApi.logout();
      void this.router.navigate(['/login']);
      return;
    }
    this.attachmentDownloadApi.requestIntent(attachmentId).subscribe({
      next: (intent) => {
        void fetch(this.attachmentDownloadApi.tokenDownloadUrl(intent.token), {
          headers: { Authorization: `Bearer ${jwt}` },
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
            if (r.status === 410) {
              throw new Error('token expired');
            }
            if (r.status === 409) {
              throw new Error('token consumed');
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
            const msg =
              e instanceof Error && e.message === 'token expired'
                ? this.i18n.instant('attachments.tokenExpired')
                : this.i18n.instant('attachments.downloadFailed');
            this.ngZone.run(() => this.toast(msg, 'error'));
          });
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(
          err.userMessage ?? this.i18n.instant('attachments.downloadFailed'),
          'error'
        );
      },
    });
  }

  // ══════════════════════════════════════════════
  // SLICE 5 — DIGITAL SIGNATURES
  // ══════════════════════════════════════════════

  /** Lazy-load signatures for the row (first toggle / pre-action gate). */
  loadSignaturesFor(att: Attachment): void {
    if (att.signatures !== undefined) {
      return;
    }
    this.signatureApi.list(att.id).subscribe({
      next: (rows) => {
        att.signatures = rows;
        this.cdr.markForCheck();
      },
      error: () => {
        att.signatures = [];
      },
    });
  }

  /** Sign the current version on behalf of the logged-in user. */
  signAttachment(att: Attachment): void {
    if (!this.cap.can('ATTACHMENT_SIGN_CREATE')) {
      this.toast(this.i18n.instant('errors.forbidden'), 'warning');
      return;
    }
    this.dialog
      .open(ConfirmDialogComponent, {
        width: 'min(420px, 92vw)',
        data: {
          titleKey: 'signature.sign',
          messageKey: 'signature.confirmSign',
          confirmKey: 'signature.sign',
          cancelKey: 'common.close',
        },
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.signatureApi.create(att.id).subscribe({
          next: (sig) => {
            att.signatures = [...(att.signatures ?? []), sig];
            this.toast(this.i18n.instant('signature.signedToast'), 'success');
          },
          error: (err: HttpErrorResponse & { userMessage?: string }) => {
            this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
          },
        });
      });
  }

  verifySignature(att: Attachment, sig: DocumentSignatureDto): void {
    this.signatureApi.verify(sig.id).subscribe({
      next: (updated) => {
        att.signatures = (att.signatures ?? []).map((s) => (s.id === updated.id ? updated : s));
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  revokeSignature(att: Attachment, sig: DocumentSignatureDto): void {
    if (!this.cap.can('ATTACHMENT_SIGNATURE_ADMIN')) {
      return;
    }
    this.signatureApi.revoke(sig.id).subscribe({
      next: (updated) => {
        att.signatures = (att.signatures ?? []).map((s) => (s.id === updated.id ? updated : s));
        this.toast(this.i18n.instant('signature.revokedToast'), 'success');
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.toast(err.userMessage ?? this.i18n.instant('errors.generic'), 'error');
      },
    });
  }

  /**
   * Slice 5 — true when the supplied workflow action requires a signature and the current user
   * has NOT yet signed every active attachment. Templates pin disabled state + tooltip on this.
   */
  workflowActionSignatureMissing(action: WorkflowActionAvailableDto): boolean {
    if (!action || !action.requiresSignature) {
      return false;
    }
    const atts = this.transaction?.attachments ?? [];
    if (atts.length === 0) {
      return false;
    }
    const myId = this.tokens.getUserId();
    if (!myId) {
      return true;
    }
    return atts.some((a) => {
      if (a.signatures === undefined) {
        this.loadSignaturesFor(a);
        return true;
      }
      return !a.signatures.some(
        (s) => s.signerUserId === myId && s.status === 'VALID' && s.verificationStatus === 'VERIFIED'
      );
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
  // READ TRACKING (Slice 1)
  // ══════════════════════════════════════════════

  /** Whether the current user has already acknowledged this correspondence. */
  isAcknowledged(): boolean {
    return !!this.myReadReceipt?.acknowledgedAt;
  }

  /** Whether the workspace should expose the Acknowledge button right now. */
  canShowAckButton(): boolean {
    return !!this.transaction && this.acknowledgementSupported && !this.isAcknowledged();
  }

  /** Whether the calling user may see the cross-user read-status panel. */
  canViewReadStatus(): boolean {
    return this.cap.can(this.READ_STATUS_VIEW_PERMISSION);
  }

  acknowledgeRead(): void {
    if (!this.correspondenceUuid || this.ackInProgress || this.isAcknowledged()) {
      return;
    }
    this.ackInProgress = true;
    const comment = this.ackCommentDraft.trim();
    this.readTrackingApi
      .acknowledge(this.correspondenceUuid, comment.length > 0 ? comment : null)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (receipt) => {
          this.myReadReceipt = receipt;
          this.ackCommentDraft = '';
          this.ackInProgress = false;
          this.toast(this.i18n.instant('readTracking.ackSuccess'), 'success');
          this.cdr.detectChanges();
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.ackInProgress = false;
          this.toast(err.userMessage ?? this.i18n.instant('readTracking.ackFailure'), 'error');
        },
      });
  }

  toggleReadStatusPanel(): void {
    if (!this.canViewReadStatus() || !this.correspondenceUuid) {
      return;
    }
    this.readStatusPanelOpen = !this.readStatusPanelOpen;
    if (this.readStatusPanelOpen && !this.readStatus) {
      this.loadReadStatus();
    }
  }

  private loadReadStatus(): void {
    if (!this.correspondenceUuid) {
      return;
    }
    this.readStatusLoading = true;
    this.readTrackingApi
      .readStatus(this.correspondenceUuid)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => {
          this.readStatus = summary;
          this.readStatusLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.readStatus = null;
          this.readStatusLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  readStatusDisplayName(row: CorrespondenceReadReceiptDto): string {
    const isAr = this.i18n.currentLang() !== 'en';
    return (
      (isAr ? row.fullNameAr : row.fullNameEn) ||
      (isAr ? row.fullNameEn : row.fullNameAr) ||
      row.username ||
      row.userId ||
      '—'
    );
  }

  // ══════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════

  navigateToTransaction(id: string): void {
    this.router.navigate(['/correspondence', id]);
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
