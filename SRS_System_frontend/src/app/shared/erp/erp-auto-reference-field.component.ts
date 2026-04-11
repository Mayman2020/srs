import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/**
 * Read-only reference/sequence field for create flows: empty + i18n placeholder until the server assigns an id.
 */
@Component({
  selector: 'app-erp-auto-reference-field',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  template: `
    <div class="erp-auto-ref" *ngIf="visible">
      <label class="field-label erp-auto-ref__label" [attr.for]="inputId">{{ labelKey | t }}</label>
      <input
        [id]="inputId"
        type="text"
        class="custom-input erp-auto-ref__input"
        readonly
        tabindex="-1"
        [attr.aria-readonly]="true"
        [value]="displayValue"
        [attr.placeholder]="placeholderKey | t"
      />
    </div>
  `,
  styles: [
    `
      .erp-auto-ref {
        margin: 0.75rem 0 0.25rem;
        max-width: 900px;
      }
      .erp-auto-ref__label {
        display: block;
        margin-bottom: 0.35rem;
      }
      .erp-auto-ref__input {
        cursor: not-allowed;
        background: #f9fafb;
        color: #111827;
      }
    `
  ]
})
export class ErpAutoReferenceFieldComponent {
  private static nextId = 0;

  @Input() value: string | number | null | undefined = null;
  /** i18n key for label (default: generic record reference). */
  @Input() labelKey = 'common.recordReferenceLabel';
  /** i18n key for placeholder when value is empty. */
  @Input() placeholderKey = 'common.recordReferencePendingPlaceholder';
  @Input() visible = true;

  readonly inputId = `erp-auto-ref-${ErpAutoReferenceFieldComponent.nextId++}`;

  get displayValue(): string {
    if (this.value === null || this.value === undefined) {
      return '';
    }
    const s = String(this.value).trim();
    return s;
  }
}
