import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SidebarService {

   private collapsedSubject = new BehaviorSubject<boolean>(false);
  collapsed$ = this.collapsedSubject.asObservable();

  toggle() {
        this.collapsedSubject.next(!this.collapsedSubject.value);

    if (this.isMobile()) {
      document.documentElement.classList.toggle('sidebar-open');
    } else {
      document.documentElement.classList.toggle('sidebar-collapsed');
    }
  }
 
  set(value: boolean) {
    this.collapsedSubject.next(value);
  }

  close() {
    document.documentElement.classList.remove('sidebar-open');
  }

  isMobile(): boolean {
    return window.matchMedia('(max-width: 1024px)').matches;
  }

  syncOnResize() {
    if (!this.isMobile()) {
      document.documentElement.classList.remove('sidebar-open');
    }
  }
}