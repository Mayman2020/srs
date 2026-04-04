import { Component, Input, Output, EventEmitter, forwardRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';

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
      const code = row[this.primaryKey];
      return this.lookupLabels.label(this.lookupTable, code == null ? '' : String(code));
    }
    const v = row[this.displayKey];
    return v == null ? '' : String(v);
  }
}