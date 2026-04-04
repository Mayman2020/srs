import { Pipe, PipeTransform } from '@angular/core';
import { LookupLabelsService } from '../lookup/lookup-labels.service';

/** Renders a lookup `code` using DB-backed names from {@link LookupLabelsService}. */
@Pipe({
  name: 'lk',
  standalone: true,
  pure: false,
})
export class LookupTranslatePipe implements PipeTransform {
  constructor(private readonly labels: LookupLabelsService) {}

  transform(code: string | null | undefined, table: string): string {
    return this.labels.label(table, code);
  }
}
