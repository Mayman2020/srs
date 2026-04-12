import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/**
 * Standard shell for Material dialogs: translated title, optional leading icon, close control,
 * consistent padding, and a slot for footer actions (`erpDialogActions`).
 */
@Component({
  selector: 'app-erp-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    TranslatePipe
  ],
  templateUrl: './erp-dialog.component.html',
  styleUrl: './erp-dialog.component.scss'
})
export class ErpDialogComponent {
  /** i18n key for dialog title (ignored when {@link titleText} is set). */
  @Input() titleKey = '';
  /** Plain dialog title (e.g. DB-driven); takes precedence over {@link titleKey}. */
  @Input() titleText?: string;
  /** Material icon ligature name (optional) */
  @Input() icon = '';
  /** i18n key for close button tooltip */
  @Input() closeTooltipKey = 'common.close';
  @Input() showClose = true;
  /** Wider min-width for forms or trees (default ~420px). */
  @Input() wide = false;
  /** Emitted when user clicks close (parent may also use mat-dialog-close on actions). */
  @Output() closed = new EventEmitter<void>();

  onClose(): void {
    this.closed.emit();
  }
}
