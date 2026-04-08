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

/**
 * Presentational page header row: i18n title/subtitle + projected actions.
 * Container pages supply API/state; this component has no business logic.
 */
@Component({
  selector: 'app-erp-page-toolbar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './erp-page-toolbar.component.html',
  styleUrl: './erp-page-toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpPageToolbarComponent implements OnChanges {
  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnChanges(_changes: SimpleChanges): void {
    this.cdr.markForCheck();
  }

  @Input({ required: true }) titleKey!: string;
  @Input() subtitleKey: string | null = null;
  /** Merges with host layout classes from parent pages. */
  @Input() containerClass = '';

  toolbarClasses(): string[] {
    return ['erp-page-toolbar', this.containerClass].filter((c) => !!c?.trim());
  }
}
