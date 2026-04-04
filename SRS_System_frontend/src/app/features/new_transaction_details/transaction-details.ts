import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';

import { WorkflowHistoryEntryDto } from '../../core/api/api-types';
import { TransactionService } from '../../services/transaction.service';

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

  ],
  standalone: true,
})
export class TransactionDetailsComponent implements OnInit, OnDestroy {

  // ── Data ────────────────────────────────────
  transaction!: Transaction;
  relatedTransactions: RelatedTransaction[] = [];

  // ── UI State ────────────────────────────────
  activeIndex = 2;
  newNote = '';
  canRefer = true;
  canAddNote = true;
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
    private transactionService: TransactionService
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
    return this.activeIndex;
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

    forkJoin({
      c: this.transactionService.getById(id),
      h: this.transactionService
        .getWorkflowHistory(id)
        .pipe(catchError(() => of([] as WorkflowHistoryEntryDto[]))),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ c, h }) => {
          const steps = this.transactionService.historyToTimeline(h).map((s) => ({
            action: s.action,
            note: s.note ?? '',
            user: s.user,
            date: s.date,
          }));
          const created = c.createdAt;
          const due = new Date(created.getTime() + c.maxDays * 86_400_000);
          this.transaction = {
            id: c.referenceNumber?.trim() ? c.referenceNumber : c.id,
            subject: c.subject,
            type: c.typeCode,
            created,
            dueDate: due,
            secrecy: c.secrecy?.trim() ? c.secrecy : c.priorityCode ?? '—',
            from: c.from?.trim() ? c.from : '—',
            to: c.to?.trim() ? c.to : '—',
            status: c.statusCode,
            statusClass: this.mapStatusClass(c.statusCode),
            maxDays: c.maxDays,
            remainingDays: Math.max(
              0,
              Math.ceil((due.getTime() - Date.now()) / 86_400_000)
            ),
            priority: c.priorityCode,
            priorityClass: 'normal',
            priorityPercent: 40,
            currentHandler: '—',
            timeline: steps,
            attachments: [],
            notes: this.notesFromHistory(h),
          };
          this.activeIndex = steps.length > 0 ? steps.length - 1 : 0;
          this.checkOverdue();
        },
        error: () => {
          this.transaction = {
            id: id,
            subject: 'تعذّر تحميل المعاملة',
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

  private notesFromHistory(h: WorkflowHistoryEntryDto[]): TransactionNote[] {
    return h
      .filter((e) => (e.primaryCommentText ?? '').trim().length > 0)
      .map((e) => ({
        id: e.id,
        author: e.actorDisplayName ?? e.actorUserId ?? '—',
        text: e.primaryCommentText!.trim(),
        date: new Date(e.occurredAt),
        typeClass: 'info',
      }));
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

  approveTransaction(): void {
    // this.transactionService.approve(this.transaction!.id).subscribe(...)
    console.log('approve');
  }

  transferTransaction(): void {
    // open transfer dialog
    console.log('transfer');
  }

  returnTransaction(): void {
    console.log('return');
  }

  referTransaction(): void {
    console.log('refer');
  }

  rejectTransaction(): void {
    console.log('reject');
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
    const content = this.form.get('letterContent')?.value;
    console.log('save draft:', content);
  }

  sendReply(): void {
    const content = this.form.get('letterContent')?.value;
    console.log('send reply:', content);
  }

  // ══════════════════════════════════════════════
  // ATTACHMENTS
  // ══════════════════════════════════════════════

  addAttachment(): void {
    console.log('add attachment');
  }

  previewAttachment(att: Attachment): void {
    window.open(att.url, '_blank');
  }

  downloadAttachment(att: Attachment): void {
    const a = document.createElement('a');
    a.href = att.url;
    a.download = att.name;
    a.click();
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
    if (!this.newNote?.trim() || !this.transaction) return;
    this.transaction.notes.push({
      id: Date.now(),
      author: 'المستخدم الحالي',
      text: this.newNote.trim(),
      date: new Date(),
      typeClass: 'info',
    });
    this.newNote = '';
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



