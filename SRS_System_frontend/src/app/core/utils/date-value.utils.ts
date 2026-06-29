/** API/storage dates as yyyy-MM-dd; UI displays dd/MM/yyyy via Material datepicker. */

export function parseApiDate(value: string | Date | null | undefined): Date | null {
  if (value === null || value === undefined || value === '') return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;

  const trimmed = String(value).trim();
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(trimmed);
  if (iso) {
    const d = new Date(+iso[1], +iso[2] - 1, +iso[3]);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  const slash = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(trimmed);
  if (slash) {
    const d = new Date(+slash[3], +slash[2] - 1, +slash[1]);
    return Number.isNaN(d.getTime()) || d.getDate() !== +slash[1] ? null : d;
  }

  const parsed = new Date(trimmed);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export function formatApiDate(value: Date | null | undefined): string {
  if (!value || Number.isNaN(value.getTime())) return '';
  const y = value.getFullYear();
  const m = String(value.getMonth() + 1).padStart(2, '0');
  const d = String(value.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}
