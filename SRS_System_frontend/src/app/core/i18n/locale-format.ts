import { toLatinDigits } from './digit-normalization.util';

export const LATIN_NUMBER_LOCALE = 'en-US';
export const ARABIC_LATIN_DIGITS_LANG = 'ar-u-nu-latn';

export function formatDateTime(
  value: Date | string | number | null | undefined,
  options?: Intl.DateTimeFormatOptions,
  lang: 'ar' | 'en' = 'en'
): string {
  if (value == null || value === '') return '—';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  const locale = lang === 'ar' ? ARABIC_LATIN_DIGITS_LANG : 'en-GB';
  return toLatinDigits(new Intl.DateTimeFormat(locale, options).format(date));
}

export const DATE_DISPLAY_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric'
};

export const DATE_TIME_DISPLAY_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
};
