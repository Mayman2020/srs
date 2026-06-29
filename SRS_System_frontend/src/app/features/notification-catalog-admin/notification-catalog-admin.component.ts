import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import {
  NotificationCatalogAdminDto,
  NotificationCatalogAdminItemDto,
  NotificationCatalogApiService,
  UpsertNotificationCatalogItemRequestDto
} from '../../core/api/notification-catalog-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { NotificationService } from '../../core/services/notification.service';
import { DialogService } from '../../core/services/dialog.service';

type CatalogKind = 'eventType' | 'channel';

@Component({
  selector: 'app-notification-catalog-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    TranslatePipe,
    ErpPageShellComponent,
    SrsDataTableComponent
  ],
  templateUrl: './notification-catalog-admin.component.html',
  styleUrl: './notification-catalog-admin.component.scss'
})
export class NotificationCatalogAdminComponent implements OnInit {
  private readonly api = inject(NotificationCatalogApiService);
  private readonly i18n = inject(I18nService);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);
  private readonly fb = inject(FormBuilder);

  catalog: NotificationCatalogAdminDto | null = null;
  loading = true;
  submitting = false;
  readonly editing = signal<{ kind: CatalogKind; code: string } | null>(null);
  readonly formKind = signal<CatalogKind>('eventType');

  readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(64)]),
    nameAr: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    nameEn: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    sortOrder: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    active: this.fb.nonNullable.control(true)
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.loadAdmin().subscribe({
      next: (c) => {
        this.catalog = c;
        this.loading = false;
      },
      error: () => {
        this.catalog = null;
        this.loading = false;
        this.toast.error('notificationCatalogAdmin.loadFailed');
      }
    });
  }

  label(item: NotificationCatalogAdminItemDto): string {
    return this.i18n.currentLang() === 'en' ? item.nameEn : item.nameAr;
  }

  startCreate(kind: CatalogKind): void {
    this.formKind.set(kind);
    this.editing.set(null);
    this.form.enable();
    this.form.reset({ code: '', nameAr: '', nameEn: '', sortOrder: 0, active: true });
  }

  startEdit(kind: CatalogKind, item: NotificationCatalogAdminItemDto): void {
    this.formKind.set(kind);
    this.editing.set({ kind, code: item.code });
    this.form.enable();
    this.form.get('code')?.disable();
    this.form.patchValue({
      code: item.code,
      nameAr: item.nameAr,
      nameEn: item.nameEn,
      sortOrder: item.sortOrder,
      active: item.active
    });
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.form.enable();
    this.form.reset({ code: '', nameAr: '', nameEn: '', sortOrder: 0, active: true });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const body: UpsertNotificationCatalogItemRequestDto = {
      code: raw.code.trim().toUpperCase(),
      nameAr: raw.nameAr.trim(),
      nameEn: raw.nameEn.trim(),
      sortOrder: Number(raw.sortOrder),
      active: !!raw.active
    };
    const kind = this.formKind();
    const edit = this.editing();
    this.submitting = true;

    const req =
      kind === 'eventType'
        ? edit
          ? this.api.updateEventType(edit.code, body)
          : this.api.createEventType(body)
        : edit
          ? this.api.updateChannel(edit.code, body)
          : this.api.createChannel(body);

    req.subscribe({
      next: () => {
        this.submitting = false;
        this.toast.success(edit ? 'notificationCatalogAdmin.saved' : 'notificationCatalogAdmin.created');
        this.cancelEdit();
        this.reload();
      },
      error: () => {
        this.submitting = false;
        this.toast.error('notificationCatalogAdmin.saveFailed');
      }
    });
  }

  confirmDelete(kind: CatalogKind, item: NotificationCatalogAdminItemDto): void {
    this.dialog
      .openConfirm({
        titleKey: 'notificationCatalogAdmin.deleteTitle',
        messageKey: 'notificationCatalogAdmin.deleteMessage',
        params: { code: item.code },
        confirmButton: { labelKey: 'common.delete', color: 'warn' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        const req =
          kind === 'eventType' ? this.api.deleteEventType(item.code) : this.api.deleteChannel(item.code);
        req.subscribe({
          next: () => {
            this.toast.success('notificationCatalogAdmin.deleted');
            this.reload();
          },
          error: () => this.toast.error('notificationCatalogAdmin.deleteFailed')
        });
      });
  }
}
