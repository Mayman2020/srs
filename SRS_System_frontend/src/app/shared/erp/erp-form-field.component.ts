import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/** Consistent label + control + validation spacing for forms (including inside dialogs). */
@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  template: `
    <div class="erp-form-field">
      @if (labelKey) {
        <label class="erp-form-field__label" [attr.for]="inputId">{{ labelKey | t }}</label>
      }
      <div class="erp-form-field__control">
        <ng-content></ng-content>
      </div>
      @if (errorKey) {
        <div class="erp-form-field__error" role="alert">{{ errorKey | t }}</div>
      }
    </div>
  `,
  styles: [
    `
      .erp-form-field {
        display: flex;
        flex-direction: column;
        gap: 6px;
        margin-bottom: 14px;
      }
      .erp-form-field__label {
        font-size: 13px;
        font-weight: 700;
        color: var(--text-primary, #1f2937);
      }
      .erp-form-field__control :ng-deep input,
      .erp-form-field__control :ng-deep select,
      .erp-form-field__control :ng-deep textarea {
        width: 100%;
        border-radius: var(--erp-radius-md, 10px);
        border: 1px solid var(--border-color, #e5e7eb);
        padding: 8px 12px;
        font: inherit;
      }
      .erp-form-field__error {
        font-size: 12px;
        color: var(--error-color, #dc2626);
      }
    `
  ]
})
export class ErpFormFieldComponent {
  @Input() labelKey = '';
  @Input() errorKey = '';
  @Input() inputId = '';
}
