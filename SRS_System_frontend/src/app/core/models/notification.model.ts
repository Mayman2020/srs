export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export interface NotificationParams {
  [key: string]: string | number;
}

export interface ToastNotification {
  id: string;
  type: NotificationType;
  messageKey?: string;
  messageText?: string;
  params?: NotificationParams;
  durationMs: number;
  createdAt: number;
}
