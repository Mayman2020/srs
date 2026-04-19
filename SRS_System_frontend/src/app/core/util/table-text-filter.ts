/**
 * Client-side "search all columns" for simple tables.
 * Normalizes Arabic/English text, removes diacritics, and performs case-insensitive inclusion.
 */
export function normalizeTableSearchText(
  value: string | number | boolean | null | undefined
): string {
  const raw = value === null || value === undefined ? '' : String(value);
  if (!raw.trim()) {
    return '';
  }

  return raw
    .normalize('NFKD')
    .replace(/[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]/g, '')
    .replace(/\u0640/g, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ى/g, 'ي')
    .replace(/ة/g, 'ه')
    .replace(/[ؤئ]/g, 'ء')
    .replace(/[٠-٩]/g, (digit) => String('٠١٢٣٤٥٦٧٨٩'.indexOf(digit)))
    .replace(/[^\p{L}\p{N}]+/gu, ' ')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ');
}

export function matchesTableSearch(
  query: string,
  parts: Array<string | number | boolean | null | undefined>
): boolean {
  const q = normalizeTableSearchText(query);
  if (!q) {
    return true;
  }

  const blob = parts
    .map((part) => normalizeTableSearchText(part))
    .join('\u0001')
    .trim();

  return blob.includes(q);
}
