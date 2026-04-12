import { Injectable, inject } from '@angular/core';
import { formatDate } from '@angular/common';
import { I18nService } from './i18n.service';
import { toLatinDigits } from './digit-normalization.util';

@Injectable({ providedIn: 'root' })
export class UiFormatService {
  private readonly i18n = inject(I18nService);

  normalizeDigits(value: string | number | null | undefined): string {
    return toLatinDigits(value);
  }

  formatNumber(
    value: number | string | null | undefined,
    options?: Intl.NumberFormatOptions
  ): string {
    const numeric = typeof value === 'number' ? value : Number(value ?? 0);
    if (!Number.isFinite(numeric)) {
      return this.normalizeDigits(value);
    }

    const locale = this.i18n.currentLang() === 'ar' ? 'ar-u-nu-latn' : 'en-US';
    return new Intl.NumberFormat(locale, options).format(numeric);
  }

  formatDate(
    value: string | number | Date | null | undefined,
    format: string,
    timezone?: string
  ): string {
    if (value === null || value === undefined || value === '') {
      return '';
    }

    const locale = this.i18n.currentLang() === 'ar' ? 'ar' : 'en';
    return this.normalizeDigits(formatDate(value, format, locale, timezone));
  }

  formatTime(
    value: string | number | Date | null | undefined,
    options: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit' }
  ): string {
    if (value === null || value === undefined || value === '') {
      return '';
    }

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const locale = this.i18n.currentLang() === 'ar' ? 'ar-u-nu-latn' : 'en-GB';
    return this.normalizeDigits(new Intl.DateTimeFormat(locale, options).format(date));
  }
}

