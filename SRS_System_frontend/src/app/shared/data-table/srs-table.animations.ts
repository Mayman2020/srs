import { animate, style, transition, trigger } from '@angular/animations';

/** Subtle row entrance when bound to `<tr>` with `*ngFor`. */
export const srsTableRowEnter = trigger('srsTableRowEnter', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(6px)' }),
    animate('200ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 1, transform: 'none' })),
  ]),
]);
