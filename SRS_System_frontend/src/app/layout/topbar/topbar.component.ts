import { Component, ContentChild, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.css'
})
export class TopbarComponent {

  @Input() pageTitle!: string;
  @Input() pageSubtitle!: string;
  @Input() actionLabel!: string;
  @Output() action = new EventEmitter<void>();

  @ContentChild('topbarAction') projectedContent?: ElementRef;

  hasProjectedAction = false;

  userName = '';

  isMobile = window.innerWidth <= 1024;

  constructor(
    private router: Router,
    private i18n: I18nService
  ) {
    this.userName = this.i18n.instant('topbar.demoUserName');
  }




  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 1024;
  }

  logout(): void {
    this.router.navigate(['/login'])
    // if (confirm('هل أنت متأكد من تسجيل الخروج؟')) {
    //   // logout logic
    // }
  }

  showNotifications = false;

  notifications = [
    { type: 'تنبيه', text: 'معاملة 1445/10293 اقتربت مدة الرد', time: '2025-02-19', read: false, important: true },
    { type: 'تذكير', text: 'معاملة 1445/10297 تحتاج متابعة', time: '2025-02-18', read: false, important: false }
  ];

  get unreadNotifications(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
  }



  toggleRead(index: number) {
    this.notifications[index].read =
      !this.notifications[index].read;
  }

  deleteNotification(index: number) {
    this.notifications.splice(index, 1);
  }

  openNotificationsPage() {
    this.showNotifications = false;
    this.router.navigate(['/notifications']);
  }

  markAllRead() {
    this.notifications.forEach(n => n.read = true);
  }
}
