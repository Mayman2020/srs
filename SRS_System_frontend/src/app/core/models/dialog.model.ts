import { Type } from '@angular/core';
import { MatDialogConfig } from '@angular/material/dialog';

export interface DialogActionConfig {
  labelKey: string;
  color?: 'primary' | 'accent' | 'warn';
}

export interface ConfirmDialogConfig {
  titleKey: string;
  messageKey: string;
  params?: Record<string, string | number>;
  confirmButton?: DialogActionConfig;
  cancelButton?: DialogActionConfig;
  width?: MatDialogConfig['width'];
  maxWidth?: MatDialogConfig['maxWidth'];
  disableClose?: boolean;
}

export interface AlertDialogConfig {
  titleKey: string;
  messageKey: string;
  params?: Record<string, string | number>;
  closeButton?: DialogActionConfig;
  width?: MatDialogConfig['width'];
  maxWidth?: MatDialogConfig['maxWidth'];
  disableClose?: boolean;
}

export interface FormDialogConfig<TData = unknown> {
  titleKey: string;
  component: Type<unknown>;
  data?: TData;
  width?: MatDialogConfig['width'];
  maxWidth?: MatDialogConfig['maxWidth'];
  disableClose?: boolean;
  panelClass?: string | string[];
}
