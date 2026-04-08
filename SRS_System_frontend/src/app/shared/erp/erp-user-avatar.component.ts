import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ERP_AVATAR_FALLBACK_ASSET } from './erp-user-profile.store';

/**
 * Presentational avatar: optional primary image, graceful fallback asset on error, else initials.
 * No API or auth logic — receives resolved URLs from a container / {@link ErpUserProfileStore}.
 */
@Component({
  selector: 'app-erp-user-avatar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './erp-user-avatar.component.html',
  styleUrl: './erp-user-avatar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpUserAvatarComponent implements OnChanges {
  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['primarySrc']) {
      this.primaryFailed = false;
      this.cdr.markForCheck();
    }
  }

  /** User photo URL (already absolute or root-relative), or null for initials-only state. */
  @Input() primarySrc: string | null = null;

  @Input() initials = '?';

  /** Shown when `primarySrc` is set but fails to load (404, CSP, etc.). */
  @Input() fallbackSrc = ERP_AVATAR_FALLBACK_ASSET;

  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  /** Optional extra class on host (e.g. topbar vs sidebar). */
  @Input() hostClass = '';

  primaryFailed = false;

  onPrimaryError(): void {
    this.primaryFailed = true;
    this.cdr.markForCheck();
  }

  hostClasses(): Record<string, boolean> {
    return {
      'erp-avatar': true,
      [`erp-avatar--${this.size}`]: true,
      [this.hostClass]: !!this.hostClass
    };
  }
}
