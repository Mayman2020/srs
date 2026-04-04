import { Pipe, PipeTransform } from '@angular/core';
import { I18nService } from './i18n.service';

/** Renders UI strings from JSON keys only (`ar.json` / `en.json`). Impure so language switches refresh. */
@Pipe({
  name: 't',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  constructor(private readonly i18n: I18nService) {}

  transform(key: string, params?: Record<string, string | number>): string {
    return this.i18n.instant(key, params);
  }
}
