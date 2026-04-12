const ARABIC_INDIC_DIGITS = /[٠-٩]/g;
const EASTERN_ARABIC_DIGITS = /[۰-۹]/g;

const ARABIC_INDIC_MAP: Record<string, string> = {
  '٠': '0',
  '١': '1',
  '٢': '2',
  '٣': '3',
  '٤': '4',
  '٥': '5',
  '٦': '6',
  '٧': '7',
  '٨': '8',
  '٩': '9',
  '۰': '0',
  '۱': '1',
  '۲': '2',
  '۳': '3',
  '۴': '4',
  '۵': '5',
  '۶': '6',
  '۷': '7',
  '۸': '8',
  '۹': '9'
};

export function toLatinDigits(value: string | number | null | undefined): string {
  if (value === null || value === undefined) {
    return '';
  }

  return String(value)
    .replace(ARABIC_INDIC_DIGITS, (digit) => ARABIC_INDIC_MAP[digit] ?? digit)
    .replace(EASTERN_ARABIC_DIGITS, (digit) => ARABIC_INDIC_MAP[digit] ?? digit)
    .replace(/\u066C/g, ',')
    .replace(/\u066B/g, '.');
}

