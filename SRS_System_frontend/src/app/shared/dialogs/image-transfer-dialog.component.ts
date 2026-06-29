import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';
import { AttachmentApiService } from '../../core/api/attachment-api.service';
import { TransactionService } from '../../core/services/transaction.service';
import { CorrespondenceAttachmentFormDto } from '../../core/api/api-types';
import { NotificationService } from '../../core/services/notification.service';
import { I18nService } from '../../core/i18n/i18n.service';

export const IMAGE_TRANSFER_PREFIX = '[IMAGE_TRANSFER]';

export interface ImageTransferDialogData {
  correspondenceId: string;
}

@Component({
  selector: 'app-image-transfer-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, TranslatePipe, ErpDialogComponent],
  template: `
    <app-erp-dialog [titleKey]="'imageTransfer.title'" icon="image">
      <p class="hint">{{ 'imageTransfer.hint' | t }}</p>
      <label class="file-picker">
        <span>{{ 'imageTransfer.pickFiles' | t }}</span>
        <input type="file" multiple accept="image/*,.pdf" (change)="onFilesPicked($event)" />
      </label>
      <ul class="file-list" *ngIf="files.length">
        <li *ngFor="let f of files">{{ f.name }}</li>
      </ul>
      <p class="error" *ngIf="errorKey">{{ errorKey | t }}</p>
      <div erpDialogActions>
        <button mat-button type="button" (click)="cancel()" [disabled]="uploading">
          {{ 'common.cancel' | t }}
        </button>
        <button mat-flat-button type="button" color="primary" (click)="upload()" [disabled]="uploading || !files.length">
          {{ uploading ? ('common.saving' | t) : ('imageTransfer.upload' | t) }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .hint {
        margin: 0 0 1rem;
        color: var(--text-muted, #6b7280);
        font-size: 0.9rem;
      }
      .file-picker {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        font-size: 0.9rem;
      }
      .file-list {
        margin: 0.75rem 0 0;
        padding-inline-start: 1.25rem;
        font-size: 0.85rem;
      }
      .error {
        color: var(--danger, #dc2626);
        margin-top: 0.75rem;
      }
    `
  ]
})
export class ImageTransferDialogComponent {
  private readonly attachmentApi = inject(AttachmentApiService);
  private readonly transactionService = inject(TransactionService);
  private readonly notification = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  files: File[] = [];
  uploading = false;
  errorKey: string | null = null;

  constructor(
    private readonly dialogRef: MatDialogRef<ImageTransferDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: ImageTransferDialogData
  ) {}

  onFilesPicked(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.files = Array.from(input.files ?? []);
    this.errorKey = null;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  upload(): void {
    if (!this.files.length || !this.data.correspondenceId) {
      return;
    }
    this.uploading = true;
    this.errorKey = null;
    const uploads$ =
      this.files.length === 0
        ? of([] as CorrespondenceAttachmentFormDto[])
        : forkJoin(
            this.files.map((file) =>
              this.attachmentApi.upload(file).pipe(
                map(
                  (up): CorrespondenceAttachmentFormDto => ({
                    displayName: `${IMAGE_TRANSFER_PREFIX} ${file.name}`,
                    storageKey: up.storageKey,
                    byteSize: up.byteSize,
                    mimeType: up.mimeType ?? undefined,
                    ...(up.plaintextSha256 ? { plaintextSha256: up.plaintextSha256 } : {}),
                    ...(up.encryptionAlgo ? { encryptionAlgo: up.encryptionAlgo } : {}),
                    ...(up.encryptionKeyRef ? { encryptionKeyRef: up.encryptionKeyRef } : {}),
                    ...(up.encryptionWrappedDekB64
                      ? { encryptionWrappedDekB64: up.encryptionWrappedDekB64 }
                      : {}),
                    ...(up.encryptionIvB64 ? { encryptionIvB64: up.encryptionIvB64 } : {}),
                    ...(up.ciphertextSha256 ? { ciphertextSha256: up.ciphertextSha256 } : {})
                  })
                )
              )
            )
          );

    uploads$
      .pipe(
        switchMap((attachments) => {
          const calls = attachments.map((a) =>
            this.transactionService.addCorrespondenceAttachment(this.data.correspondenceId, a)
          );
          return calls.length ? forkJoin(calls) : of([]);
        })
      )
      .subscribe({
        next: () => {
          this.uploading = false;
          this.notification.successRaw(this.i18n.instant('imageTransfer.success'));
          this.dialogRef.close(true);
        },
        error: () => {
          this.uploading = false;
          this.errorKey = 'imageTransfer.failed';
        }
      });
  }
}
