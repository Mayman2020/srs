import { AfterViewChecked, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { SmartAssistantService } from '../../core/services/smart-assistant.service';
import { SmartAssistantMessage, SmartAssistantAction } from '../../core/models/smart-assistant.model';
import { UiFormatService } from '../../core/i18n/ui-format.service';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-chat-bubble',
  standalone: true,
  imports: [FormsModule, CommonModule, TranslatePipe],
  templateUrl: './chat-bubble.component.html',
  styleUrl: './chat-bubble.component.css'
})
export class ChatBubbleComponent implements OnInit, AfterViewChecked {
  @ViewChild('messageViewport') private messageViewport?: ElementRef<HTMLDivElement>;

  private readonly assistant = inject(SmartAssistantService);
  private readonly i18n = inject(I18nService);
  private readonly format = inject(UiFormatService);
  private readonly router = inject(Router);

  open = false;
  query = '';
  loading = false;
  lastQuery = '';
  requestError = false;
  readonly suggestionKeys = this.assistant.suggestionKeys;
  readonly emptyActions = this.assistant.emptyStateActions;
  messages: SmartAssistantMessage[] = [];
  private shouldScroll = false;

  ngOnInit(): void {
    this.pushAssistantMessage(this.i18n.instant('chat.greetingDynamic'), this.emptyActions);
  }

  ngAfterViewChecked(): void {
    if (!this.shouldScroll) {
      return;
    }
    this.shouldScroll = false;
    const host = this.messageViewport?.nativeElement;
    if (host) {
      host.scrollTop = host.scrollHeight;
    }
  }

  toggle(): void {
    this.open = !this.open;
    this.shouldScroll = true;
  }

  timestamp(): string {
    return this.format.formatTime(new Date());
  }

  send(): void {
    const q = this.query.trim();
    if (!q || this.loading) {
      return;
    }
    this.lastQuery = q;
    this.requestError = false;
    this.messages.push(this.createMessage('user', q));
    this.messages.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      text: this.i18n.instant('chat.typing'),
      createdAt: this.timestamp(),
      pending: true
    });
    this.loading = true;
    this.query = '';
    this.shouldScroll = true;

    this.assistant
      .answer(q)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (reply) => {
          this.messages = this.messages.filter((message) => !message.pending);
          this.pushAssistantMessage(reply.text, reply.actions);
        },
        error: () => {
          this.messages = this.messages.filter((message) => !message.pending);
          this.requestError = true;
          this.messages.push({
            id: crypto.randomUUID(),
            role: 'assistant',
            text: this.i18n.instant('chat.error'),
            createdAt: this.timestamp(),
            error: true,
            actions: [{ id: 'retry', label: this.i18n.instant('chat.retry') }]
          });
          this.shouldScroll = true;
        }
      });
  }

  sendSuggestionKey(key: string): void {
    this.query = this.i18n.instant(key);
    this.send();
  }

  onAction(action: SmartAssistantAction): void {
    if (action.prompt) {
      this.query = action.prompt;
      this.send();
      return;
    }
    if (action.id === 'retry') {
      this.retry();
      return;
    }
    if (action.route) {
      void this.router.navigateByUrl(action.route);
      this.open = false;
    }
  }

  retry(): void {
    if (!this.lastQuery || this.loading) {
      return;
    }
    this.query = this.lastQuery;
    this.send();
  }

  trackMessage(_index: number, message: SmartAssistantMessage): string {
    return message.id;
  }

  private pushAssistantMessage(text: string, actions?: SmartAssistantAction[]): void {
    this.messages.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      text,
      createdAt: this.timestamp(),
      actions
    });
    this.shouldScroll = true;
  }

  private createMessage(role: 'assistant' | 'user', text: string): SmartAssistantMessage {
    return {
      id: crypto.randomUUID(),
      role,
      text,
      createdAt: this.timestamp()
    };
  }
}
