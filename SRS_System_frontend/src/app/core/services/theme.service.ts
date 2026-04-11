import { OverlayContainer } from '@angular/cdk/overlay';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly overlayContainer = inject(OverlayContainer);
  private readonly subject = new BehaviorSubject<ThemeMode>(this.readInitialTheme());

  readonly isDark$: Observable<boolean> = this.subject.pipe(
    map((mode) => mode === 'dark'),
    distinctUntilChanged()
  );

  readonly mode$: Observable<ThemeMode> = this.subject.pipe(distinctUntilChanged());

  constructor() {
    this.applyTheme(this.subject.value);
  }

  get isDark(): boolean {
    return this.subject.value === 'dark';
  }

  get mode(): ThemeMode {
    return this.subject.value;
  }

  toggle(): void {
    this.setTheme(this.subject.value === 'dark' ? 'light' : 'dark');
  }

  setTheme(mode: ThemeMode): void {
    this.subject.next(mode);
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // Ignore storage failures in private mode.
    }
    this.applyTheme(mode);
  }

  syncFromServer(theme: string | null | undefined): void {
    if (theme === 'light' || theme === 'dark') {
      this.setTheme(theme);
    }
  }

  private readInitialTheme(): ThemeMode {
    try {
      const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
      if (stored === 'light' || stored === 'dark') {
        return stored;
      }
    } catch {
      // Ignore storage failures in private mode.
    }
    if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  private applyTheme(mode: ThemeMode): void {
    if (typeof document === 'undefined') {
      return;
    }

    const overlay = this.overlayContainer.getContainerElement();
    const roots = [document.documentElement, document.body, overlay];
    for (const root of roots) {
      root.classList.remove('light-theme', 'dark-theme');
      root.classList.add(`${mode}-theme`);
      root.setAttribute('data-theme', mode);
    }
  }
}
