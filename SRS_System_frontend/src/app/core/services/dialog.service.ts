import { Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AlertDialogConfig,
  ConfirmDialogConfig,
  FormDialogConfig
} from '../models/dialog.model';
import { I18nService } from '../i18n/i18n.service';
import { AlertDialogComponent } from '../../shared/components/dialog/alert-dialog.component';
import { ConfirmDialogComponent } from '../../shared/components/dialog/confirm-dialog.component';
import { FormDialogComponent } from '../../shared/components/dialog/form-dialog.component';

@Injectable({ providedIn: 'root' })
export class DialogService {
  constructor(
    private readonly dialog: MatDialog,
    private readonly i18n: I18nService
  ) {}

  openConfirm(config: ConfirmDialogConfig): Observable<boolean> {
    return this.dialog
      .open(ConfirmDialogComponent, {
        width: config.width ?? 'min(28rem, calc(100vw - 2rem))',
        maxWidth: config.maxWidth ?? '94vw',
        autoFocus: false,
        restoreFocus: true,
        disableClose: config.disableClose ?? false,
        panelClass: ['app-dialog-panel', 'app-confirm-dialog'],
        direction: this.i18n.currentDirection,
        data: config
      })
      .afterClosed()
      .pipe(map((value) => value === true));
  }

  openAlert(config: AlertDialogConfig): MatDialogRef<AlertDialogComponent> {
    return this.dialog.open(AlertDialogComponent, {
      width: config.width ?? 'min(28rem, calc(100vw - 2rem))',
      maxWidth: config.maxWidth ?? '94vw',
      autoFocus: false,
      restoreFocus: true,
      disableClose: config.disableClose ?? false,
      panelClass: ['app-dialog-panel', 'app-alert-dialog'],
      direction: this.i18n.currentDirection,
      data: config
    });
  }

  openForm<TResult = unknown, TData = unknown>(
    config: FormDialogConfig<TData>
  ): MatDialogRef<FormDialogComponent, TResult> {
    return this.dialog.open<FormDialogComponent, FormDialogConfig<TData>, TResult>(
      FormDialogComponent,
      {
        width: config.width ?? 'min(42rem, calc(100vw - 2rem))',
        maxWidth: config.maxWidth ?? '96vw',
        autoFocus: false,
        restoreFocus: true,
        disableClose: config.disableClose ?? false,
        panelClass: ['app-dialog-panel', 'app-form-dialog', config.panelClass ?? '']
          .flat()
          .filter(Boolean),
        direction: this.i18n.currentDirection,
        data: config
      }
    );
  }
}
