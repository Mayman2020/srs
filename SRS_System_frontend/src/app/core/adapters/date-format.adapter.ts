import { NativeDateAdapter } from '@angular/material/core';
import { formatDate } from '@angular/common';

export class DateFormatAdapter extends NativeDateAdapter {
  override parse(value: string | null, _parseFormat: unknown): Date | null {
    if (!value) return null;
    const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(String(value).trim());
    if (match) {
      const day = parseInt(match[1], 10);
      const month = parseInt(match[2], 10) - 1;
      const year = parseInt(match[3], 10);
      const d = new Date(year, month, day);
      if (!isNaN(d.getTime()) && d.getDate() === day) return d;
    }
    return super.parse(value, _parseFormat);
  }
  override format(date: Date, displayFormat: string): string {
    if (!this.isValid(date)) throw Error('DateFormatAdapter: Cannot format invalid date.');
    return formatDate(date, displayFormat, this.locale as string);
  }
}
