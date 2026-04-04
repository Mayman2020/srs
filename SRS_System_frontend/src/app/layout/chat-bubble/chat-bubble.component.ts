import { Component, OnInit } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../models/transaction.model';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatIcon } from '@angular/material/icon';

import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [FormsModule, CommonModule, MatIcon],
  templateUrl: './chat-bubble.component.html',
  styleUrl: './chat-bubble.component.css',
  encapsulation: ViewEncapsulation.None
})
export class ChatBubbleComponent implements OnInit {

  open = false;
  query = '';

  /** Loaded from API; chat intents run client-side over this cache. */
  private txCache: Transaction[] = [];

  suggestions = [
    'اعرض المعاملات المتأخرة',
    'كم معاملة قيد الإجراء؟',
    'لخّص آخر 5 معاملات'
  ];

  messages: { role: string, text: SafeHtml, time: string }[] = [];


  constructor(
    private txService: TransactionService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit() {
    this.messages.push({
      role: 'bot',
      text: 'مرحباً 👋 كيف يمكنني مساعدتك؟',
      time: this.now()
    });

    this.txService.listPage().subscribe({
      next: (rows) => (this.txCache = rows),
      error: () => (this.txCache = [])
    });
  }

  toggle() {
    this.open = !this.open;
  }

  now() {
    return new Date().toLocaleTimeString('ar-SA', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  send() {

    if (!this.query.trim()) return;

    const q = this.query;

    this.messages.push({
      role: 'user',
      text: this.sanitizer.bypassSecurityTrustHtml(q),
      time: this.now()
    });
    const response = this.intentReply(q);

    this.messages.push({
      role: 'bot',
      text: this.sanitizer.bypassSecurityTrustHtml(response),
      time: this.now()
    });

    this.query = '';

  }

  sendSuggestion(text: string) {
    this.query = text;
    this.send();
  }

  isLate(tx: Transaction) {
    const created = new Date(tx.created);
    const now = new Date();
    const diffDays = (now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24);

    if (tx.statusCode === 'RETURNED') return true;
    if (tx.statusCode === 'IN_PROGRESS' && diffDays > tx.maxDays) return true;
    return false;
  }
  transactionCard(t: Transaction) {

    return `

<div class="ai-tx-card">

<div class="ai-tx-head">

<span class="ai-tx-type">
${t.type}
</span>

<span class="ai-tx-id">
${t.id}
</span>

</div>

<div class="ai-tx-subject">
${t.subject}
</div>

<div class="ai-tx-meta">

<div>
<span class="label">المرسل</span>
${t.from}
</div>

<div>
<span class="label">المستلم</span>
${t.to}
</div>

<div>
<span class="label">الحالة</span>
<span class="status">${t.status}</span>
</div>

</div>

</div>

`;
  }



  intentReply(q: string) {
    const data = this.txCache;

    const uuidMatch = q.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
    if (uuidMatch) {
      const tx = data.find((t) => t.id === uuidMatch[0]);
      if (!tx) return 'لم يتم العثور على المعاملة';
      return this.transactionCard(tx);
    }

    const legacyId = q.match(/\d{4}\/\d+/);
    if (legacyId) {
      const tx = data.find((t) => t.id === legacyId[0] || t.referenceNumber === legacyId[0]);
      if (!tx) return 'لم يتم العثور على المعاملة';
      return this.transactionCard(tx);
    }


    // البحث الذكي

    const list = this.filterTransactions(q);

    if (!list.length)
      return 'لا توجد معاملات مطابقة للبحث';


    return `
النتائج: ${list.length}

<br><br>

${list.map(t => this.transactionCard(t)).join('')}
`;

  }

  parseQuery(q: string) {
    const text = q.toLowerCase();

    return {
      type: text.includes('وارد')
        ? 'INBOUND'
        : text.includes('صادر')
          ? 'OUTBOUND'
          : text.includes('داخلية')
            ? 'INTERNAL'
            : text.includes('خارجية')
              ? 'EXTERNAL'
              : null,

      status:
        text.includes('منجزة') || text.includes('مكتملة')
          ? 'COMPLETED'
          : text.includes('قيد')
            ? 'IN_PROGRESS'
            : text.includes('جديدة')
              ? 'NEW'
              : text.includes('مرفوضة')
                ? 'REJECTED'
                : text.includes('معادة')
                  ? 'RETURNED'
                  : null,

      secrecy: text.includes('سري') ? 'SECRET' : null,

      late:
        text.includes('متأخر') || text.includes('متأخرة'),

      from:
        text.includes('من ') ? text.split('من ')[1] : null,

      to:
        text.includes('إلى ') ? text.split('إلى ')[1] : null,

      today:
        text.includes('اليوم')

    };

  }

  filterTransactions(q: string) {
    const filters = this.parseQuery(q);

    let data = [...this.txCache];

    if (filters.type) {
      data = data.filter((t) => t.typeCode === filters.type);
    }

    if (filters.status) {
      data = data.filter((t) => t.statusCode === filters.status);
    }


    if (filters.late) {

      data = data.filter(
        t => this.isLate(t)
      );

    }

    if (filters.secrecy) {

      data = data.filter(
        t => t.secrecy === filters.secrecy
      );

    }

    if (filters.from) {

      data = data.filter(
        t => t.from.includes(filters.from!)
      );

    }

    if (filters.to) {

      data = data.filter(
        t => t.to.includes(filters.to!)
      );

    }

    if (filters.today) {

      const today = new Date().toDateString();

      data = data.filter(
        t => new Date(t.created).toDateString() === today
      );

    }

    return data;

  }

}