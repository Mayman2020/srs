import { Component, Input, Output, EventEmitter, forwardRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { LookupLabelsService } from '../../../core/lookup/lookup-labels.service';
import { LookupItemDto } from '../../../core/api/api-types';

@Component({
  selector: 'app-generic-select',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './generic-select.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GenericSelectComponent),
      multi: true
    }
  ]
})
export class GenericSelectComponent implements ControlValueAccessor {
  private readonly lookupLabels = inject(LookupLabelsService);

  /** i18n key (e.g. `transactions.type`) */
  @Input() label = '';
  @Input() items: unknown[] = [];
  @Input() primaryKey = 'id';
  @Input() displayKey = 'name';
  /** i18n key for empty option */
  @Input() placeholder = 'genericSelect.placeholder';
  /** When set, option labels resolve from the lookup bundle (`LookupLabelsService`) by table key and row code. */
  @Input() lookupTable?: string;
  @Input() required = false;
  @Input() hasError = false;

  @Output() valueChange = new EventEmitter<any>();

  value: any;

  onChange = (value: any) => {};
  onTouched = () => {};

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {}

  selectValue(val: any) {
    this.value = val;
    this.onChange(val);
    this.valueChange.emit(val);
  }

  optionValue(item: unknown): unknown {
    if (!item || typeof item !== 'object') {
      return undefined;
    }
    return (item as Record<string, unknown>)[this.primaryKey];
  }

  getDisplayText(item: unknown): string {
    if (!item || typeof item !== 'object') {
      return '';
    }
    const row = item as Record<string, unknown>;
    if (this.lookupTable) {
      const codeStr = row[this.primaryKey] == null ? '' : String(row[this.primaryKey]);
      const resolved = this.lookupLabels.label(this.lookupTable, codeStr);
      if (resolved !== '\u2014' && resolved !== codeStr) {
        return resolved;
      }
    }
    const v = row[this.displayKey];
    if (v != null && String(v).trim() !== '') {
      return String(v);
    }
    const nameAr = row['nameAr'];
    const nameEn = row['nameEn'];
    if (typeof nameAr === 'string' && typeof nameEn === 'string') {
      const dto: LookupItemDto = {
        id: 0,
        code: String(row[this.primaryKey] ?? ''),
        nameAr,
        nameEn,
        sortOrder: 0
      };
      return this.lookupLabels.displayName(dto);
    }
    if (this.lookupTable) {
      const codeStr = row[this.primaryKey] == null ? '' : String(row[this.primaryKey]);
      return this.lookupLabels.label(this.lookupTable, codeStr);
    }
    return '';
  }
}
