import { CommonModule, NgComponentOutlet } from '@angular/common';
import { Component, Inject, Injector, computed, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { FormDialogConfig } from '../../../core/models/dialog.model';
import { FORM_DIALOG_DATA } from './dialog.tokens';
import { DialogShellComponent } from './dialog-shell.component';

@Component({
  selector: 'app-form-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, NgComponentOutlet, DialogShellComponent],
  templateUrl: './form-dialog.component.html'
})
export class FormDialogComponent {
  private readonly injector = inject(Injector);

  readonly contentInjector = computed(() =>
    Injector.create({
      providers: [{ provide: FORM_DIALOG_DATA, useValue: this.data.data }],
      parent: this.injector
    })
  );

  constructor(
    private readonly dialogRef: MatDialogRef<FormDialogComponent, unknown>,
    @Inject(MAT_DIALOG_DATA) readonly data: FormDialogConfig
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
