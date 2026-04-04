import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';

export type AppLang = 'ar' | 'en';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly http = inject(HttpClient);

  private translations: Record<string, unknown> = {};
  readonly currentLang = signal<AppLang>('ar');

  /** Load JSON dictionary for the given language (e.g. `/assets/i18n/ar.json`). */
  loadLang(lang: AppLang): Observable<void> {
    return this.http.get<Record<string, unknown>>(`/assets/i18n/${lang}.json`).pipe(
      tap((data) => {
        this.translations = data ?? {};
        this.currentLang.set(lang);
        if (typeof document !== 'undefined') {
          document.documentElement.lang = lang;
          document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
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
