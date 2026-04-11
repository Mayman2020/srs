import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { ClickOutsideDirective } from '../../../directives/click-outside.directive';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

export interface HeaderLanguageItem {
  code: string;
  labelKey: string;
  nativeLabelKey: string;
  flagUrl: string;
}

export interface HeaderNotificationItem {
  id: string;
  title: string;
  message: string;
  timeLabel: string;
  read: boolean;
  correspondenceId?: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatToolbarModule,
    MatTooltipModule,
    ClickOutsideDirective,
    TranslatePipe
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  @Input() profileName = '';
  @Input() profileInitials = '';
  @Input() profileAvatarUrl: string | null = null;
  @Input() activeLanguage!: HeaderLanguageItem;
  @Input() languages: readonly HeaderLanguageItem[] = [];
  @Input() notifications: readonly HeaderNotificationItem[] = [];
  @Input() unreadCount = 0;
  @Input() notificationsOpen = false;
  @Input() isDark = false;
  @Input() submenuXPosition: 'before' | 'after' = 'after';

  @Output() notificationsToggle = new EventEmitter<void>();
  @Output() notificationsClose = new EventEmitter<void>();
  @Output() notificationSelected = new EventEmitter<HeaderNotificationItem>();
  @Output() markAllNotificationsRead = new EventEmitter<void>();
  @Output() themeToggle = new EventEmitter<void>();
  @Output() languageSelected = new EventEmitter<string>();
  @Output() logout = new EventEmitter<void>();

  trackNotification(_index: number, item: HeaderNotificationItem): string {
    return item.id;
  }
}
