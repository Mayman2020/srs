import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';

import { CorrespondenceCommentDetailDto, CorrespondenceDetailResponse, WorkflowHistoryEntryDto } from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';
import { AttachmentApiService } from '../../core/api/attachment-api.service';
import { AuthTokenService } from '../../core/auth/auth-token.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { HttpErrorResponse } from '@angular/common/http';

import { MatTabsModule } from '@angular/material/tabs';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { EditorModule } from '@tinymce/tinymce-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { VisualWorkflowDialogComponent } from '../visual-workflow-dialog/visual-workflow-dialog.component';



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
  created: Date | string;
  status: string;
  statusClass: 'active' | 'done' | 'pending' | 'rejected';
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
  statusClass: 'active' | 'done' | 'pending' | 'rejected';
  maxDays: number;
  remainingDays: number;
  priority?: string;
  priorityClass?: 'low' | 'normal' | 'high' | 'urgent';
  priorityPercent?: number;
  currentHandler: string;
  timeline: TimelineStep[];
  attachments: Attachment[];
  notes: TransactionNote[];
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
    TranslatePipe,
  ],
  standalone: true,
})
export class TransactionDetailsComponent implements OnInit, OnDestroy {

  // ── Data ────────────────────────────────────
  transaction!: Transaction;
  relatedTransactions: RelatedTransaction[] = [];
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
    private attachmentApi: AttachmentApiService,
    private tokens: AuthTokenService,
    private i18n: I18nService
  ) {}

  // ══════════════════════════════════════════════
  // LIFECYCLE
  // ══════════════════════════════════════════════

  ngOnInit(): void {
    this.buildForm();
    this.loadTransaction();
    this.loadRelated();
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
      h: this.transactionService
        .getWorkflowHistory(id)
        .pipe(catchError(() => of([] as WorkflowHistoryEntryDto[]))),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ d, h }) => {
          this.transaction = this.mapDetail(d, h);
          const steps = this.transaction.timeline.length;
          this.activeIndex = steps > 0 ? steps - 1 : 0;
          this.checkOverdue();
          this.canAddNote = true;
        },
        error: () => {
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
            statusClass: 'pending',
            maxDays: 0,
            remainingDays: 0,
            priority: '—',
            priorityClass: 'normal',
            priorityPercent: 0,
            currentHandler: '—',
            timeline: [],
            attachments: [],
            notes: [],
          };
        },
      });
  }

  private mapDetail(d: CorrespondenceDetailResponse, h: WorkflowHistoryEntryDto[]): Transaction {
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
    const typeCode = d.correspondenceType?.code ?? '—';
    const statusCode = d.correspondenceStatus?.code ?? '—';
    const priorityCode = d.priority?.code ?? '—';
    const secrecyCode = d.confidentiality?.code ?? '—';

    return {
      id: d.id,
      referenceNumber: d.referenceNumber ?? d.id,
      subject: d.subject ?? '—',
      type: typeCode,
      created,
      dueDate,
      secrecy: secrecyCode,
      from: d.senderOrganization?.nameAr ?? d.senderOrganization?.nameEn ?? '—',
      to: d.recipientOrganization?.nameAr ?? d.recipientOrganization?.nameEn ?? '—',
      status: statusCode,
      statusClass: this.mapStatusClass(statusCode),
      maxDays,
      remainingDays: Math.max(0, Math.ceil((dueDate.getTime() - Date.now()) / 86_400_000)),
      priority: priorityCode,
      priorityClass: 'normal',
      priorityPercent: 40,
      currentHandler: d.ownerDepartment?.nameAr ?? d.ownerDepartment?.code ?? '—',
      timeline: steps,
      attachments: (d.attachments ?? []).map((a) => ({
        id: a.id,
        name: a.displayName,
        type: a.contentType?.code ?? 'FILE',
        secrecy: secrecyCode,
        size: this.formatBytes(
          (a.versions ?? []).reduce((m, v) => Math.max(m, v.byteSize), 0)
        ),
        date: (a.versions?.[0]?.createdAt ?? d.updatedAt ?? '').toString().substring(0, 10),
        url: this.attachmentApi.downloadUrl(a.id),
      })),
      notes: this.notesFromComments(d.comments ?? []),
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

  private loadRelated(): void {
    this.relatedTransactions = [];
  }

  private mapStatusClass(
    code: string
  ): 'active' | 'done' | 'pending' | 'rejected' {
    const u = code.toUpperCase();
    if (u.includes('COMPLET') || u.includes('DONE') || u.includes('CLOS'))
      return 'done';
    if (u.includes('REJECT')) return 'rejected';
    if (u.includes('NEW') || u.includes('DRAFT')) return 'pending';
    return 'active';
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

  private runWorkflowAction(
    action: 'APPROVE' | 'REJECT' | 'RETURN',
    comment?: string | null
  ): void {
    if (!this.correspondenceUuid) {
      return;
    }
    this.transactionService
      .workflowAction(this.correspondenceUuid, { action, comment })
      .subscribe({
        next: () => this.loadTransaction(),
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          const msg = err.userMessage ?? this.i18n.instant('errors.generic');
          window.alert(msg);
        },
      });
  }

  approveTransaction(): void {
    this.runWorkflowAction('APPROVE');
  }

  /** Quick complete (same as approve) — e.g. toolbar "تحويل". */
  transferTransaction(): void {
    this.runWorkflowAction('APPROVE');
  }

  returnTransaction(): void {
    const c = window.prompt(this.i18n.instant('transactionDetails.workflowCommentPrompt'));
    if (c === null) {
      return;
    }
    if (!c.trim()) {
      window.alert(this.i18n.instant('transactionDetails.workflowCommentRequired'));
      return;
    }
    this.runWorkflowAction('RETURN', c);
  }

  referTransaction(): void {
    this.returnTransaction();
  }

  rejectTransaction(): void {
    const c = window.prompt(this.i18n.instant('transactionDetails.workflowCommentPrompt'));
    if (c === null) {
      return;
    }
    if (!c.trim()) {
      window.alert(this.i18n.instant('transactionDetails.workflowCommentRequired'));
      return;
    }
    this.runWorkflowAction('REJECT', c);
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
    /* Persisted via correspondence APIs when available */
  }

  sendReply(): void {
    /* Outbound reply via correspondence APIs when available */
  }

  // ══════════════════════════════════════════════
  // ATTACHMENTS
  // ══════════════════════════════════════════════

  addAttachment(): void {
    /* Upload wired when correspondence attachment API is exposed for existing items */
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
      return;
    }
    fetch(this.attachmentApi.downloadUrl(attachmentId), {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (!r.ok) {
          throw new Error('download failed');
        }
        return r.blob();
      })
      .then((blob) => {
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
      })
      .catch(() => {
        window.alert(this.i18n.instant('errors.generic'));
      });
  }

  deleteAttachment(att: Attachment): void {
    if (!this.transaction) return;
    this.transaction.attachments =
      this.transaction.attachments.filter(a => a.id !== att.id);
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
        window.alert(err.userMessage ?? this.i18n.instant('errors.generic'));
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
  if (!this.transaction) return;

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



