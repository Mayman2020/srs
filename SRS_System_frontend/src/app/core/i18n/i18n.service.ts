import { OverlayContainer } from '@angular/cdk/overlay';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';

export type AppLang = 'ar' | 'en';

/** Language row for header selector (flags + native label keys). */
export interface LanguageOption {
  code: AppLang;
  /** i18n key for label in current UI language */
  labelKey: string;
  /** i18n key for native name (e.g. "English", "العربية") */
  nativeLabelKey: string;
  flagUrl: string;
}

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly http = inject(HttpClient);
  private readonly overlay = inject(OverlayContainer);

  private translations: Record<string, unknown> = {};
  readonly currentLang = signal<AppLang>('ar');

  readonly languages: LanguageOption[] = [
    {
      code: 'ar',
      labelKey: 'languages.ar.label',
      nativeLabelKey: 'languages.ar.native',
      flagUrl: 'assets/flags/sa.svg'
    },
    {
      code: 'en',
      labelKey: 'languages.en.label',
      nativeLabelKey: 'languages.en.native',
      flagUrl: 'assets/flags/gb.svg'
    }
  ];

  /** Load JSON dictionary for the given language (e.g. `/assets/i18n/ar.json`). */
  loadLang(lang: AppLang): Observable<void> {
    return this.http.get<Record<string, unknown>>(`/assets/i18n/${lang}.json`).pipe(
      tap((data) => {
        this.translations = data ?? {};
        this.currentLang.set(lang);
        if (typeof document !== 'undefined') {
          const dir = lang === 'ar' ? 'rtl' : 'ltr';
          document.documentElement.lang = lang;
          document.documentElement.dir = dir;
          document.body?.setAttribute('dir', dir);
          try {
            this.overlay.getContainerElement().setAttribute('dir', dir);
          } catch {
            /* ignore */
          }
        }
        try {
          localStorage.setItem('lang', lang);
        } catch {
          /* ignore */
        }
      }),
      map(() => undefined)
    );
  }

  get currentDirection(): 'rtl' | 'ltr' {
    return this.currentLang() === 'ar' ? 'rtl' : 'ltr';
  }

  /**
   * Resolve `a.b.c` against nested JSON; returns the key path if missing.
   * Optional `{{name}}` placeholders in the string are replaced from `params`.
   */
  instant(key: string, params?: Record<string, string | number>): string {
    const parts = key.split('.').filter(Boolean);
    let cur: unknown = this.translations;
    for (const p of parts) {
      if (cur === null || typeof cur !== 'object' || !(p in (cur as object))) {
        return key;
      }
      cur = (cur as Record<string, unknown>)[p];
    }
    let s = typeof cur === 'string' ? cur : key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        s = s.split(`{{${k}}}`).join(String(v));
      }
    }
    return s;
  }

}
