import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import {
  DATE_DISPLAY_OPTIONS,
  DATE_TIME_DISPLAY_OPTIONS,
  formatDateTime
} from '../../core/i18n/locale-format';

@Pipe({ name: 'srsDate', standalone: true, pure: false })
export class SrsDatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(
    value: Date | string | number | null | undefined,
    mode: 'date' | 'datetime' = 'date'
  ): string {
    this.i18n.currentLang();
    const options = mode === 'date' ? DATE_DISPLAY_OPTIONS : DATE_TIME_DISPLAY_OPTIONS;
    return formatDateTime(value, options, this.i18n.currentLang());
  }
}
