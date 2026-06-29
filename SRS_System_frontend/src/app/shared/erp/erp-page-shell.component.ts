import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ErpPageToolbarComponent } from './erp-page-toolbar.component';

/** Standard page wrapper: toolbar + padded content region. */
@Component({
  selector: 'app-erp-page-shell',
  standalone: true,
  imports: [CommonModule, ErpPageToolbarComponent],
  template: `
    <section class="erp-page-shell" [class.erp-page-shell--narrow]="narrow">
      <app-erp-page-toolbar [titleKey]="titleKey" [subtitleKey]="subtitleKey">
        <div erpToolbarActions>
          <ng-content select="[shellActions]"></ng-content>
        </div>
        <div erpToolbarMeta>
          <ng-content select="[shellMeta]"></ng-content>
        </div>
      </app-erp-page-toolbar>
      <div class="erp-page-shell__body">
        <ng-content></ng-content>
      </div>
    </section>
  `,
  styles: [
    `
      .erp-page-shell {
        width: 100%;
        min-width: 0;
        max-width: var(--content-max, 1280px);
        margin-inline: auto;
        padding: var(--page-pad, 1.25rem) clamp(1rem, 2.5vw, 1.5rem) 2rem;
      }
      .erp-page-shell--narrow {
        max-width: min(var(--content-max, 1280px), 48rem);
      }
      .erp-page-shell__body {
        margin-top: 1rem;
        min-width: 0;
        max-width: 100%;
      }
      @media (max-width: 1024px) {
        .erp-page-shell {
          padding-bottom: 1.5rem;
        }
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpPageShellComponent {
  @Input({ required: true }) titleKey!: string;
  @Input() subtitleKey: string | null = null;
  @Input() narrow = false;
}
