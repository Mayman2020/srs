import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { AlertDialogConfig } from '../../../core/models/dialog.model';
import { DialogShellComponent } from './dialog-shell.component';

@Component({
  selector: 'app-alert-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    TranslatePipe,
    DialogShellComponent
  ],
  templateUrl: './alert-dialog.component.html'
})
export class AlertDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<AlertDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: AlertDialogConfig
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
