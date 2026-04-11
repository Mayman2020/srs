import { Component, OnInit } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../models/transaction.model';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';

import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [FormsModule, CommonModule, TranslatePipe],
  templateUrl: './chat-bubble.component.html',
  styleUrl: './chat-bubble.component.css',
  encapsulation: ViewEncapsulation.None
})
export class ChatBubbleComponent implements OnInit {
  open = false;
  query = '';

  /** i18n keys for quick-reply chips (resolved in template via `| t`). */
  readonly suggestionKeys = ['chat.suggestion1', 'chat.suggestion2', 'chat.suggestion3'] as const;

  /** Loaded from API; chat intents run client-side over this cache. */
  private txCache: Transaction[] = [];

  messages: { role: string; text: SafeHtml; time: string }[] = [];

  constructor(
    private txService: TransactionService,
    private sanitizer: DomSanitizer,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.messages.push({
      role: 'bot',
      text: this.sanitizer.bypassSecurityTrustHtml(this.i18n.instant('chat.greeting')),
      time: this.now()
    });

    this.txService.listPage().subscribe({
      next: (rows) => (this.txCache = rows),
      error: () => (this.txCache = [])
    });
  }

  toggle(): void {
    this.open = !this.open;
  }

  now(): string {
    const loc = this.i18n.currentLang() === 'en' ? 'en-GB' : 'ar-SA';
    return new Date().toLocaleTimeString(loc, {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  send(): void {
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

  /** Sends the translated suggestion text as the user query (intent parsing uses both AR/EN tokens). */
  sendSuggestionKey(key: string): void {
    this.query = this.i18n.instant(key);
    this.send();
  }

  isLate(tx: Transaction): boolean {
    const created = new Date(tx.created);
    const now = new Date();
    const diffDays = (now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24);

    if (tx.statusCode === 'RETURNED') return true;
    if (tx.statusCode === 'IN_PROGRESS' && diffDays > tx.maxDays) return true;
    return false;
  }

  private intentMatches(text: string, tokenKey: string): boolean {
    const raw = this.i18n.instant(`chat.intentTokens.${tokenKey}`).toLowerCase();
    const parts = raw
      .split('|')
      .map((s) => s.trim())
      .filter(Boolean);
    const t = text.toLowerCase();
    return parts.some((p) => t.includes(p));
  }

  private extractAfterAny(text: string, tokenKey: string): string | null {
    const lower = text.toLowerCase();
    const raw = this.i18n.instant(`chat.intentTokens.${tokenKey}`);
    for (const marker of raw
      .split('|')
      .map((s) => s.trim())
      .filter(Boolean)) {
      const idx = lower.indexOf(marker.toLowerCase());
      if (idx >= 0) return text.slice(idx + marker.length).trim();
    }
    return null;
  }

  transactionCard(t: Transaction): string {
    const sender = this.i18n.instant('chat.labelSender');
    const recipient = this.i18n.instant('chat.labelRecipient');
    const status = this.i18n.instant('chat.labelStatus');
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
<span class="label">${sender}</span>
${t.from}
</div>

<div>
<span class="label">${recipient}</span>
${t.to}
</div>

<div>
<span class="label">${status}</span>
<span class="status">${t.status}</span>
</div>

</div>

</div>

`;
  }

  intentReply(q: string): string {
    const data = this.txCache;

    const uuidMatch = q.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
    if (uuidMatch) {
      const tx = data.find((t) => t.id === uuidMatch[0]);
      if (!tx) return this.i18n.instant('chat.notFound');
      return this.transactionCard(tx);
    }

    const legacyId = q.match(/\d{4}\/\d+/);
    if (legacyId) {
      const tx = data.find((t) => t.id === legacyId[0] || t.referenceNumber === legacyId[0]);
      if (!tx) return this.i18n.instant('chat.notFound');
      return this.transactionCard(tx);
    }

    const list = this.filterTransactions(q);

    if (!list.length) return this.i18n.instant('chat.noMatches');

    const header = this.i18n.instant('chat.resultsHeader', { count: list.length });
    return `
${header}

<br><br>

${list.map((t) => this.transactionCard(t)).join('')}
`;
  }

  parseQuery(q: string): {
    type: string | null;
    status: string | null;
    secrecy: string | null;
    late: boolean;
    from: string | null;
    to: string | null;
    today: boolean;
  } {
    const text = q.toLowerCase();

    return {
      type: this.intentMatches(text, 'inbound')
        ? 'INBOUND'
        : this.intentMatches(text, 'outbound')
          ? 'OUTBOUND'
          : this.intentMatches(text, 'internal')
            ? 'INTERNAL'
            : this.intentMatches(text, 'external')
              ? 'EXTERNAL'
              : null,

      status: this.intentMatches(text, 'statusCompleted')
        ? 'COMPLETED'
        : this.intentMatches(text, 'statusInProgress')
          ? 'IN_PROGRESS'
          : this.intentMatches(text, 'statusNew')
            ? 'NEW'
            : this.intentMatches(text, 'statusRejected')
              ? 'REJECTED'
              : this.intentMatches(text, 'statusReturned')
                ? 'RETURNED'
                : null,

      secrecy: this.intentMatches(text, 'secrecySecret') ? 'SECRET' : null,

      late: this.intentMatches(text, 'late'),

      from: this.extractAfterAny(q, 'fromMarker'),

      to: this.extractAfterAny(q, 'toMarker'),

      today: this.intentMatches(text, 'today')
    };
  }

  filterTransactions(q: string): Transaction[] {
    const filters = this.parseQuery(q);

    let data = [...this.txCache];

    if (filters.type) {
      data = data.filter((t) => t.typeCode === filters.type);
    }

    if (filters.status) {
      data = data.filter((t) => t.statusCode === filters.status);
    }

    if (filters.late) {
      data = data.filter((t) => this.isLate(t));
    }

    if (filters.secrecy) {
      data = data.filter((t) => t.secrecy === filters.secrecy);
    }

    if (filters.from) {
      data = data.filter((t) => t.from.includes(filters.from!));
    }

    if (filters.to) {
      data = data.filter((t) => t.to.includes(filters.to!));
    }

    if (filters.today) {
      const today = new Date().toDateString();
      data = data.filter((t) => new Date(t.created).toDateString() === today);
    }

    return data;
  }
}
