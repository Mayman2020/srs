import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { NotificationService } from '../../../core/services/notification.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss',
  animations: [
    trigger('toastMotion', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translate3d(0, -12px, 0) scale(0.98)' }),
        animate('220ms cubic-bezier(0.2, 0.8, 0.2, 1)', style({ opacity: 1, transform: 'translate3d(0, 0, 0) scale(1)' }))
      ]),
      transition(':leave', [
        animate('180ms ease-in', style({ opacity: 0, transform: 'translate3d(0, -10px, 0) scale(0.98)' }))
      ])
    ])
  ]
})
export class ToastComponent {
  readonly notification = inject(NotificationService);

  dismiss(id: string): void {
    this.notification.dismiss(id);
  }

  iconFor(type: string): string {
    switch (type) {
      case 'success':
        return 'check_circle';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      default:
        return 'info';
    }
  }
}
