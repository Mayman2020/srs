import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'rms-icon-btn',
  standalone: true,
  imports: [NgIf, RouterLink, MatTooltipModule, TranslateModule],
  template: `
    <a
      *ngIf="routerLink; else actionBtn"
      class="rms-icon-btn"
      [class.rms-icon-btn--primary]="variant === 'primary'"
      [class.rms-icon-btn--gold]="variant === 'gold'"
      [class.rms-icon-btn--warn]="variant === 'warn'"
      [class.rms-icon-btn--danger]="variant === 'danger'"
      [routerLink]="routerLink"
      [attr.aria-label]="ariaLabel || (tooltipKey | translate)"
      [matTooltip]="tooltip || (tooltipKey | translate)"
      [matTooltipPosition]="tooltipPosition"
      [matTooltipShowDelay]="200">
      <span class="material-icons">{{ icon }}</span>
    </a>
    <ng-template #actionBtn>
      <button
        type="button"
        class="rms-icon-btn"
        [class.rms-icon-btn--primary]="variant === 'primary'"
        [class.rms-icon-btn--gold]="variant === 'gold'"
        [class.rms-icon-btn--warn]="variant === 'warn'"
        [class.rms-icon-btn--danger]="variant === 'danger'"
        [disabled]="disabled"
        [attr.aria-label]="ariaLabel || (tooltipKey | translate)"
        [matTooltip]="tooltip || (tooltipKey | translate)"
        [matTooltipPosition]="tooltipPosition"
        [matTooltipShowDelay]="200"
        (click)="clicked.emit($event)">
        <span class="material-icons">{{ icon }}</span>
      </button>
    </ng-template>
  `
})
export class RmsIconBtnComponent {
  @Input() icon = 'help';
  @Input() tooltip = '';
  @Input() tooltipKey = '';
  @Input() ariaLabel = '';
  @Input() variant: 'default' | 'primary' | 'gold' | 'warn' | 'danger' = 'default';
  @Input() tooltipPosition: 'above' | 'below' | 'left' | 'right' | 'before' | 'after' = 'below';
  @Input() disabled = false;
  @Input() routerLink: string | any[] | null = null;
  @Output() clicked = new EventEmitter<MouseEvent>();
}
