import { Injectable, signal } from '@angular/core';
import { NotificationParams, NotificationType, ToastNotification } from '../models/notification.model';

const DEFAULT_DURATION_MS = 4500;
/** Same toast key within this window is ignored (e.g. double HTTP + manual success). */
const DEDUPE_WINDOW_MS = 2800;
const MAX_STACK = 4;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly itemsSignal = signal<ToastNotification[]>([]);
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();
  private lastSignature = '';
  private lastShownAt = 0;

  readonly items = this.itemsSignal.asReadonly();

  success(messageKey: string, params?: NotificationParams): void {
    this.enqueue('success', { messageKey, params });
  }

  error(messageKey: string, params?: NotificationParams): void {
    this.enqueue('error', { messageKey, params });
  }

  warning(messageKey: string, params?: NotificationParams): void {
    this.enqueue('warning', { messageKey, params });
  }

  info(messageKey: string, params?: NotificationParams): void {
    this.enqueue('info', { messageKey, params });
  }

  successRaw(messageText: string): void {
    this.enqueue('success', { messageText });
  }

  errorRaw(messageText: string): void {
    this.enqueue('error', { messageText });
  }

  warningRaw(messageText: string): void {
    this.enqueue('warning', { messageText });
  }

  infoRaw(messageText: string): void {
    this.enqueue('info', { messageText });
  }

  dismiss(id: string): void {
    this.clearTimer(id);
    this.itemsSignal.update((items) => items.filter((item) => item.id !== id));
  }

  clear(): void {
    for (const id of this.timers.keys()) {
      this.clearTimer(id);
    }
    this.itemsSignal.set([]);
  }

  private enqueue(
    type: NotificationType,
    payload: { messageKey?: string; messageText?: string; params?: NotificationParams }
  ): void {
    const messageKey = payload.messageKey?.trim();
    const messageText = payload.messageText?.trim();
    if (!messageKey && !messageText) {
      return;
    }

    const signature = buildToastSignature(type, messageKey, messageText, payload.params);
    const now = Date.now();

    const duplicateInStack = this.itemsSignal().some((item) => itemSignature(item) === signature);
    if (duplicateInStack) {
      return;
    }
    if (signature === this.lastSignature && now - this.lastShownAt < DEDUPE_WINDOW_MS) {
      return;
    }
    this.lastSignature = signature;
    this.lastShownAt = now;

    const item: ToastNotification = {
      id: crypto.randomUUID(),
      type,
      messageKey,
      messageText,
      params: payload.params,
      durationMs: DEFAULT_DURATION_MS,
      createdAt: now
    };

    this.itemsSignal.update((items) => {
      const next = [...items, item];
      while (next.length > MAX_STACK) {
        const removed = next.shift();
        if (removed) {
          this.clearTimer(removed.id);
        }
      }
      return next;
    });
    this.timers.set(
      item.id,
      setTimeout(() => this.dismiss(item.id), item.durationMs)
    );
  }

  private clearTimer(id: string): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
  }
}

function buildToastSignature(
  type: NotificationType,
  messageKey: string | undefined,
  messageText: string | undefined,
  params: NotificationParams | undefined
): string {
  return `${type}|${messageKey ?? ''}|${messageText ?? ''}|${JSON.stringify(params ?? {})}`;
}

function itemSignature(item: ToastNotification): string {
  return buildToastSignature(item.type, item.messageKey, item.messageText, item.params);
}
