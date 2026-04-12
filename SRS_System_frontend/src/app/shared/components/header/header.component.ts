import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink } from '@angular/router';
import { ClickOutsideDirective } from '../../../directives/click-outside.directive';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { ErpUserAvatarComponent } from '../../erp/erp-user-avatar.component';
import { DarkLightModeSwitchMainComponent } from '../dark-light-mode-switch-main/dark-light-mode-switch-main.component';

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

export interface HeaderRoleItem {
  code: string;
  label: string;
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
    ClickOutsideDirective,
    TranslatePipe,
    ErpUserAvatarComponent,
    DarkLightModeSwitchMainComponent
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  @Input() profileName = '';
  @Input() profileRole = '';
  @Input() profileInitials = '';
  @Input() profileAvatarUrl: string | null = null;
  @Input() activeLanguage!: HeaderLanguageItem;
  @Input() languages: readonly HeaderLanguageItem[] = [];
  @Input() notifications: readonly HeaderNotificationItem[] = [];
  @Input() unreadCount = 0;
  @Input() notificationsOpen = false;
  @Input() isDark = false;
  @Input() submenuXPosition: 'before' | 'after' = 'after';
  @Input() roleOptions: readonly HeaderRoleItem[] = [];
  @Input() currentRoleCode: string | null = null;
  @Input() roleSwitching = false;

  @Output() notificationsToggle = new EventEmitter<void>();
  @Output() notificationsClose = new EventEmitter<void>();
  @Output() notificationSelected = new EventEmitter<HeaderNotificationItem>();
  @Output() markAllNotificationsRead = new EventEmitter<void>();
  @Output() menuToggle = new EventEmitter<void>();
  @Output() themeToggle = new EventEmitter<void>();
  @Output() languageSelected = new EventEmitter<string>();
  @Output() roleSelected = new EventEmitter<string>();
  @Output() logout = new EventEmitter<void>();

  trackNotification(_index: number, item: HeaderNotificationItem): string {
    return item.id;
  }

  trackRole(_index: number, item: HeaderRoleItem): string {
    return item.code;
  }

  get hasNotifications(): boolean {
    return this.notifications.length > 0;
  }

  get showRoleSwitcher(): boolean {
    return this.roleOptions.length > 0;
  }

  selectRole(roleCode: string): void {
    if (!roleCode || this.roleSwitching || roleCode === this.currentRoleCode) {
      return;
    }
    this.roleSelected.emit(roleCode);
  }
}
