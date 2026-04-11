import { HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import {
  SKIP_SUCCESS_NOTIFICATION,
  SUCCESS_MESSAGE_KEY,
} from './http-notification-context';

export const successNotificationInterceptor: HttpInterceptorFn = (req, next) => {
  const notification = inject(NotificationService);

  return next(req).pipe(
    tap((event) => {
      if (event.type !== HttpEventType.Response) {
        return;
      }
      if (req.context.get(SKIP_SUCCESS_NOTIFICATION)) {
        return;
      }
      const messageKey = resolveSuccessKey(req.method, req.context.get(SUCCESS_MESSAGE_KEY));
      if (messageKey) {
        notification.success(messageKey);
      }
    })
  );
};

function resolveSuccessKey(method: string, overrideKey: string | null): string | null {
  if (overrideKey) {
    return overrideKey;
  }
  switch (method) {
    case 'POST':
      return 'notification.create.success';
    case 'PUT':
    case 'PATCH':
      return 'notification.save.success';
    case 'DELETE':
      return 'notification.delete.success';
    default:
      return null;
  }
}
