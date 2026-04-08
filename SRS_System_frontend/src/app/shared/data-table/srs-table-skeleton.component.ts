import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'srs-table-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="srs-sk" role="status" [attr.aria-label]="ariaLabel || null">
      <table class="srs-sk__table">
        <tbody>
          <tr *ngFor="let _ of rowArr; let ri = index" class="srs-sk__row" [style.animation-delay.ms]="ri * 40">
            <td *ngFor="let __ of colArr" class="srs-sk__cell">
              <div class="srs-sk__pulse"></div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [
    `
      .srs-sk {
        padding: 2px;
      }
      .srs-sk__table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 6px;
      }
      .srs-sk__cell {
        padding: 0;
        border: none;
        vertical-align: middle;
      }
      .srs-sk__pulse {
        height: 14px;
        border-radius: 6px;
        background: linear-gradient(
          90deg,
          rgba(148, 163, 184, 0.22) 0%,
          rgba(148, 163, 184, 0.38) 50%,
          rgba(148, 163, 184, 0.22) 100%
        );
        background-size: 200% 100%;
        animation: srs-sk-shimmer 1.1s ease-in-out infinite;
      }
      .srs-sk__row {
        animation: srs-sk-fade 0.35s ease-out both;
      }
      @keyframes srs-sk-shimmer {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }
      @keyframes srs-sk-fade {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }
    `,
  ],
})
export class SrsTableSkeletonComponent {
  @Input() rows = 5;
  @Input() columns = 6;
  /** Optional accessible label; parent should pass a translated string. */
  @Input() ariaLabel = '';

  get rowArr(): number[] {
    return Array.from({ length: Math.max(1, this.rows) }, (_, i) => i);
  }

  get colArr(): number[] {
    return Array.from({ length: Math.max(1, this.columns) }, (_, i) => i);
  }
}
