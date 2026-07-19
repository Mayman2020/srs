import { Injectable } from '@angular/core';
import { Location } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, filter } from 'rxjs';

/** Tracks in-app navigation so page header back only appears after drill-down. */
@Injectable({ providedIn: 'root' })
export class NavigationHistoryService {
  private stack: string[] = [];
  private resetOnNextNav = false;
  private readonly canGoBackSubject = new BehaviorSubject<boolean>(false);
  readonly canGoBack$ = this.canGoBackSubject.asObservable();

  constructor(private readonly router: Router) {
    router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe((e) => {
      this.onNavigate(e.urlAfterRedirects.split('?')[0]);
    });
  }

  /** Call before sidebar / top-level menu navigation — clears back on the target page. */
  markFromMenu(): void {
    this.resetOnNextNav = true;
  }

  canGoBack(): boolean {
    return this.stack.length > 1;
  }

  goBack(location: Location): void {
    if (!this.canGoBack()) return;
    location.back();
  }

  private onNavigate(url: string): void {
    if (this.resetOnNextNav) {
      this.stack = [url];
      this.resetOnNextNav = false;
      this.emit();
      return;
    }

    const existingIdx = this.stack.lastIndexOf(url);
    if (existingIdx >= 0 && existingIdx < this.stack.length - 1) {
      this.stack = this.stack.slice(0, existingIdx + 1);
      this.emit();
      return;
    }

    const last = this.stack[this.stack.length - 1];
    if (last !== url) {
      this.stack.push(url);
    }
    this.emit();
  }

  private emit(): void {
    this.canGoBackSubject.next(this.canGoBack());
  }
}
