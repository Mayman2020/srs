import { OverlayContainer } from '@angular/cdk/overlay';
import { Injectable, inject, isDevMode } from '@angular/core';
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
    document.documentElement.style.colorScheme = mode;

    if (isDevMode()) {
      this.runThemeAudit(mode);
    }
  }

  private runThemeAudit(mode: ThemeMode): void {
    requestAnimationFrame(() => {
      if (mode !== 'dark') {
        return;
      }
      const all = Array.from(document.querySelectorAll<HTMLElement>('body *'));
      const readableIssues: Array<{ ratio: number; element: HTMLElement }> = [];
      const whiteSurfaceIssues: HTMLElement[] = [];

      for (const el of all) {
        if (!this.isVisible(el)) {
          continue;
        }

        const style = getComputedStyle(el);
        const fg = this.parseRgb(style.color);
        const bg = this.resolveEffectiveBackground(el);

        if (bg && this.isNearWhite(bg)) {
          whiteSurfaceIssues.push(el);
        }

        if (!fg || !bg) {
          continue;
        }
        const ratio = this.contrastRatio(fg, bg);
        if (ratio < 4.5) {
          readableIssues.push({ ratio, element: el });
        }
      }

      if (whiteSurfaceIssues.length || readableIssues.length) {
        console.groupCollapsed(
          `[ThemeAudit] dark-mode issues: ${readableIssues.length} low-contrast, ${whiteSurfaceIssues.length} white surfaces`
        );
        if (readableIssues.length) {
          const top = readableIssues
            .sort((a, b) => a.ratio - b.ratio)
            .slice(0, 12)
            .map((entry) => ({
              ratio: Number(entry.ratio.toFixed(2)),
              element: this.describe(entry.element)
            }));
          console.table(top);
        }
        if (whiteSurfaceIssues.length) {
          console.table(
            whiteSurfaceIssues.slice(0, 12).map((el) => ({
              element: this.describe(el)
            }))
          );
        }
        console.groupEnd();
      }
    });
  }

  private isVisible(el: HTMLElement): boolean {
    const style = getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
      return false;
    }
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  }

  private resolveEffectiveBackground(el: HTMLElement): [number, number, number] | null {
    let current: HTMLElement | null = el;
    while (current) {
      const style = getComputedStyle(current);
      const bg = this.parseRgb(style.backgroundColor);
      if (bg && !this.isTransparent(style.backgroundColor)) {
        return bg;
      }
      current = current.parentElement;
    }
    const rootBg = this.parseRgb(getComputedStyle(document.body).backgroundColor);
    return rootBg;
  }

  private parseRgb(input: string): [number, number, number] | null {
    if (!input || input === 'transparent') {
      return null;
    }
    const m = input.match(/rgba?\(([^)]+)\)/i);
    if (!m) {
      return null;
    }
    const parts = m[1]
      .split(',')
      .map((v) => Number(v.trim()))
      .filter((v) => Number.isFinite(v));
    if (parts.length < 3) {
      return null;
    }
    return [parts[0], parts[1], parts[2]];
  }

  private isTransparent(input: string): boolean {
    if (input === 'transparent') {
      return true;
    }
    const m = input.match(/rgba\(([^)]+)\)/i);
    if (!m) {
      return false;
    }
    const parts = m[1].split(',').map((v) => Number(v.trim()));
    return parts.length === 4 && parts[3] === 0;
  }

  private isNearWhite(rgb: [number, number, number]): boolean {
    return rgb[0] >= 245 && rgb[1] >= 245 && rgb[2] >= 245;
  }

  private contrastRatio(a: [number, number, number], b: [number, number, number]): number {
    const l1 = this.luminance(a);
    const l2 = this.luminance(b);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
  }

  private luminance(rgb: [number, number, number]): number {
    const toLinear = (c: number) => {
      const v = c / 255;
      return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
    };
    return 0.2126 * toLinear(rgb[0]) + 0.7152 * toLinear(rgb[1]) + 0.0722 * toLinear(rgb[2]);
  }

  private describe(el: HTMLElement): string {
    const id = el.id ? `#${el.id}` : '';
    const cls = (el.className || '')
      .toString()
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((c) => `.${c}`)
      .join('');
    return `${el.tagName.toLowerCase()}${id}${cls}`;
  }
}
