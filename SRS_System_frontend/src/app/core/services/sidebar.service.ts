import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/** Matches `--bp-md` in global styles (1024px). */
export const SIDEBAR_MOBILE_MEDIA = '(max-width: 1024px)';

@Injectable({ providedIn: 'root' })
export class SidebarService {
  private readonly collapsedSubject = new BehaviorSubject<boolean>(false);
  private readonly mobileOpenSubject = new BehaviorSubject<boolean>(false);

  readonly collapsed$ = this.collapsedSubject.asObservable();
  readonly mobileOpen$ = this.mobileOpenSubject.asObservable();

  toggle(): void {
    if (this.isMobile()) {
      this.setMobileOpen(!this.mobileOpenSubject.value);
      return;
    }
    this.setCollapsed(!this.collapsedSubject.value);
  }

  set(value: boolean): void {
    if (this.isMobile()) {
      this.setMobileOpen(value);
      return;
    }
    this.setCollapsed(value);
  }

  close(): void {
    this.setMobileOpen(false);
  }

  isMobile(): boolean {
    return window.matchMedia(SIDEBAR_MOBILE_MEDIA).matches;
  }

  syncOnResize(): void {
    if (!this.isMobile()) {
      this.setMobileOpen(false);
      return;
    }
    document.documentElement.classList.remove('sidebar-collapsed');
  }

  private setCollapsed(value: boolean): void {
    this.collapsedSubject.next(value);
    document.documentElement.classList.toggle('sidebar-collapsed', value);
  }

  private setMobileOpen(value: boolean): void {
    this.mobileOpenSubject.next(value);
    document.documentElement.classList.toggle('sidebar-open', value);
    document.documentElement.classList.toggle('sidebar-scroll-lock', value);
  }
}
