import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

@Component({
  selector: 'app-dialog-shell',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslatePipe
  ],
  templateUrl: './dialog-shell.component.html',
  styleUrl: './dialog-shell.component.scss'
})
export class DialogShellComponent {
  @Input() titleKey = '';
  @Input() closeTooltipKey = 'common.close';
  @Input() icon = '';
  @Input() wide = false;
  @Input() showClose = true;
  @Output() closeRequested = new EventEmitter<void>();

  onClose(): void {
    this.closeRequested.emit();
  }
}
