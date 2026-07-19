import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges
} from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Subscription } from 'rxjs';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NavigationHistoryService } from '../../core/services/navigation-history.service';

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
export class ErpPageToolbarComponent implements OnChanges, OnInit, OnDestroy {
  constructor(
    private readonly cdr: ChangeDetectorRef,
    private readonly navHistory: NavigationHistoryService,
    private readonly location: Location
  ) {}

  ngOnChanges(_changes: SimpleChanges): void {
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    this.syncBack();
    this.backSub = this.navHistory.canGoBack$.subscribe(() => this.syncBack());
  }

  ngOnDestroy(): void {
    this.backSub?.unsubscribe();
  }

  @Input({ required: true }) titleKey!: string;
  @Input() subtitleKey: string | null = null;
  /** Merges with host layout classes from parent pages. */
  @Input() containerClass = '';
  /** When true, show smart back when navigation stack allows it. */
  @Input() showBack = false;

  canGoBack = false;
  private backSub?: Subscription;

  toolbarClasses(): string[] {
    return ['erp-page-toolbar', this.containerClass].filter((c) => !!c?.trim());
  }

  onBack(): void {
    this.navHistory.goBack(this.location);
  }

  private syncBack(): void {
    this.canGoBack = this.showBack && this.navHistory.canGoBack();
    this.cdr.markForCheck();
  }
}
