import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpPageShellComponent } from '../../shared/erp/erp-page-shell.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import {
  OutboundDeliveryApiService,
  OutboundDeliveryDto,
  UpsertOutboundDeliveryRequestDto
} from '../../core/api/outbound-delivery-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { DialogService } from '../../core/services/dialog.service';

@Component({
  selector: 'app-outbound-delivery',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, ErpPageShellComponent, SrsDataTableComponent],
  templateUrl: './outbound-delivery.component.html',
  styleUrl: './outbound-delivery.component.scss'
})
export class OutboundDeliveryComponent implements OnInit {
  private readonly api = inject(OutboundDeliveryApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(NotificationService);
  private readonly dialog = inject(DialogService);

  rows: OutboundDeliveryDto[] = [];
  loading = true;
  modalOpen = false;
  editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    correspondenceId: ['', Validators.required],
    channelCode: ['EMAIL', Validators.required],
    statusCode: ['PENDING', Validators.required],
    recipientLabel: [''],
    proofReference: [''],
    notes: [''],
    sentDate: [''],
    sentTime: [''],
    deliveredDate: [''],
    deliveredTime: ['']
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.list().subscribe({
      next: (rows) => {
        this.rows = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
        this.toast.error('outboundDelivery.loadFailed');
      }
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      correspondenceId: '',
      channelCode: 'EMAIL',
      statusCode: 'PENDING',
      recipientLabel: '',
      proofReference: '',
      notes: '',
      sentDate: '', sentTime: '', deliveredDate: '', deliveredTime: ''
    });
    this.modalOpen = true;
  }

  openEdit(row: OutboundDeliveryDto): void {
    this.editingId = row.id;
    this.form.reset({
      correspondenceId: row.correspondenceId,
      channelCode: row.channelCode,
      statusCode: row.statusCode,
      recipientLabel: row.recipientLabel ?? '',
      proofReference: row.proofReference ?? '',
      notes: row.notes ?? '',
      sentDate: row.sentAt ? row.sentAt.slice(0, 10) : '',
      sentTime: row.sentAt ? row.sentAt.slice(11, 16) : '',
      deliveredDate: row.deliveredAt ? row.deliveredAt.slice(0, 10) : '',
      deliveredTime: row.deliveredAt ? row.deliveredAt.slice(11, 16) : ''
    });
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }
    const body = this.toBody();
    const req = this.editingId
      ? this.api.update(this.editingId, body)
      : this.api.create(body);
    req.subscribe({
      next: () => {
        this.modalOpen = false;
        this.toast.success(this.editingId ? 'outboundDelivery.updated' : 'outboundDelivery.created');
        this.reload();
      },
      error: () => this.toast.error('outboundDelivery.saveFailed')
    });
  }

  deleteRow(row: OutboundDeliveryDto): void {
    this.dialog
      .openConfirm({
        titleKey: 'outboundDelivery.deleteTitle',
        messageKey: 'outboundDelivery.deleteMessage',
        confirmButton: { labelKey: 'common.delete', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.api.delete(row.id).subscribe({
          next: () => {
            this.toast.success('outboundDelivery.deleted');
            this.reload();
          },
          error: () => this.toast.error('outboundDelivery.deleteFailed')
        });
      });
  }

  private toBody(): UpsertOutboundDeliveryRequestDto {
    const v = this.form.getRawValue();
    return {
      correspondenceId: v.correspondenceId.trim(),
      channelCode: v.channelCode.trim(),
      statusCode: v.statusCode.trim(),
      recipientLabel: v.recipientLabel.trim() || null,
      proofReference: v.proofReference.trim() || null,
      notes: v.notes.trim() || null,
      sentAt: this.toIso(v.sentDate, v.sentTime),
      deliveredAt: this.toIso(v.deliveredDate, v.deliveredTime)
    };
  }

  private toIso(date: string, time: string): string | null {
    if (!date) return null;
    return new Date(`${date}T${time || '00:00'}:00`).toISOString();
  }
}
