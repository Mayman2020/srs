import { Pipe, PipeTransform } from '@angular/core';
import { toLatinDigits } from './digit-normalization.util';

@Pipe({
  name: 'latinDigits',
  standalone: true,
  pure: true
})
export class LatinDigitsPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    return toLatinDigits(value);
  }
}
