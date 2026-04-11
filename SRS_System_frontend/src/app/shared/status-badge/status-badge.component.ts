import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LookupTranslatePipe } from '../../core/i18n/lookup-translate.pipe';
import { correspondenceStatusBadgeClass } from '../../core/util/correspondence-status-ui';

/**
 * Correspondence lifecycle status badge: styles from DB `ui_variant` only (no code-based CSS).
 * Use `plain` when the label is not a lookup code (e.g. link kind on related items).
 */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule, LookupTranslatePipe],
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.scss',
})
export class StatusBadgeComponent {
  @Input({ required: true }) code!: string;
  @Input() uiVariant: string | null | undefined;
  /** When true, show {@link code} as plain text (still uses {@link uiVariant} for colors). */
  @Input() plain = false;
  @Input() size: 'sm' | 'md' = 'sm';

  badgeClass(): string {
    return correspondenceStatusBadgeClass(this.uiVariant);
  }
}
