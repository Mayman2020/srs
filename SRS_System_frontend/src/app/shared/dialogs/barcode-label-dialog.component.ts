import { AfterViewInit, Component, ElementRef, Inject, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import JsBarcode from 'jsbarcode';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ErpDialogComponent } from '../erp/erp-dialog.component';

export interface BarcodeLabelItem {
  reference: string;
  subject?: string | null;
}

export interface BarcodeLabelDialogData {
  items: BarcodeLabelItem[];
}

type LabelSizeKey = '50x30' | '70x40' | '100x50';

const LABEL_SIZES: Record<LabelSizeKey, { widthMm: number; heightMm: number }> = {
  '50x30': { widthMm: 50, heightMm: 30 },
  '70x40': { widthMm: 70, heightMm: 40 },
  '100x50': { widthMm: 100, heightMm: 50 }
};

@Component({
  selector: 'app-barcode-label-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    TranslatePipe,
    ErpDialogComponent
  ],
  template: `
    <app-erp-dialog [titleKey]="'barcodeLabel.title'" icon="qr_code_2">
      <div class="barcode-toolbar">
        <mat-form-field appearance="outline" class="barcode-toolbar__size">
          <mat-label>{{ 'barcodeLabel.size' | t }}</mat-label>
          <mat-select [(ngModel)]="labelSize" (selectionChange)="onSizeChange()">
            <mat-option value="50x30">50 × 30 mm</mat-option>
            <mat-option value="70x40">70 × 40 mm</mat-option>
            <mat-option value="100x50">100 × 50 mm</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div class="barcode-labels" [attr.data-size]="labelSize">
        <article class="barcode-label" *ngFor="let item of data.items; let i = index">
          <p class="barcode-label__ref">{{ item.reference }}</p>
          <p class="barcode-label__subject" *ngIf="item.subject">{{ item.subject }}</p>
          <svg #barcodeSvg class="barcode-label__svg" [attr.data-index]="i"></svg>
        </article>
      </div>

      <iframe #printFrame class="barcode-print-frame" title="print"></iframe>

      <div erpDialogActions>
        <button mat-button type="button" (click)="close()">{{ 'common.close' | t }}</button>
        <button mat-stroked-button type="button" (click)="downloadZpl()" [disabled]="!data.items.length">
          {{ 'barcodeLabel.downloadZpl' | t }}
        </button>
        <button mat-flat-button type="button" color="primary" (click)="printLabels()">
          {{ 'barcodeLabel.print' | t }}
        </button>
      </div>
    </app-erp-dialog>
  `,
  styles: [
    `
      .barcode-toolbar {
        margin-bottom: 0.75rem;
      }
      .barcode-toolbar__size {
        width: min(100%, 14rem);
      }
      .barcode-labels {
        display: grid;
        gap: 1rem;
        max-height: min(70vh, 520px);
        overflow: auto;
      }
      .barcode-label {
        border: 1px dashed var(--border, #d1d5db);
        border-radius: 8px;
        padding: 0.75rem 1rem;
        text-align: center;
        background: #fff;
        color: #111;
        margin: 0 auto;
      }
      .barcode-labels[data-size='50x30'] .barcode-label {
        width: 50mm;
        min-height: 30mm;
      }
      .barcode-labels[data-size='70x40'] .barcode-label {
        width: 70mm;
        min-height: 40mm;
      }
      .barcode-labels[data-size='100x50'] .barcode-label {
        width: 100mm;
        min-height: 50mm;
      }
      .barcode-label__ref {
        font-weight: 700;
        margin: 0 0 0.25rem;
        font-size: 1rem;
      }
      .barcode-label__subject {
        margin: 0 0 0.5rem;
        font-size: 0.85rem;
        color: #374151;
      }
      .barcode-label__svg {
        width: 100%;
        max-width: 280px;
        height: 72px;
      }
      .barcode-print-frame {
        position: fixed;
        width: 0;
        height: 0;
        border: 0;
        visibility: hidden;
      }
    `
  ]
})
export class BarcodeLabelDialogComponent implements AfterViewInit {
  @ViewChildren('barcodeSvg') barcodeSvgs!: QueryList<ElementRef<SVGElement>>;
  @ViewChild('printFrame') printFrame?: ElementRef<HTMLIFrameElement>;

  labelSize: LabelSizeKey = '70x40';

  constructor(
    private readonly dialogRef: MatDialogRef<BarcodeLabelDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: BarcodeLabelDialogData
  ) {}

  ngAfterViewInit(): void {
    requestAnimationFrame(() => this.renderBarcodes());
  }

  close(): void {
    this.dialogRef.close();
  }

  onSizeChange(): void {
    requestAnimationFrame(() => this.renderBarcodes());
  }

  printLabels(): void {
    const size = LABEL_SIZES[this.labelSize];
    const labelsHtml = this.data.items
      .map((item) => {
        const subject = item.subject
          ? `<p class="subject">${this.escapeHtml(item.subject)}</p>`
          : '';
        return `<article class="label"><p class="ref">${this.escapeHtml(item.reference)}</p>${subject}<p class="ref">${this.escapeHtml(item.reference)}</p></article>`;
      })
      .join('');

    const doc = `
      <!doctype html>
      <html>
        <head>
          <meta charset="utf-8" />
          <title>Labels</title>
          <style>
            @page { size: ${size.widthMm}mm ${size.heightMm}mm; margin: 2mm; }
            body { margin: 0; font-family: Arial, sans-serif; }
            .label {
              width: ${size.widthMm - 4}mm;
              min-height: ${size.heightMm - 4}mm;
              box-sizing: border-box;
              page-break-after: always;
              text-align: center;
              padding: 2mm;
            }
            .ref { font-weight: 700; margin: 0 0 1mm; font-size: 10pt; }
            .subject { margin: 0 0 1mm; font-size: 8pt; color: #333; }
          </style>
        </head>
        <body>${labelsHtml}</body>
      </html>`;

    const frame = this.printFrame?.nativeElement;
    if (!frame?.contentWindow) {
      window.print();
      return;
    }
    const win = frame.contentWindow;
    const docRef = win.document;
    docRef.open();
    docRef.write(doc);
    docRef.close();
    win.focus();
    win.print();
  }

  downloadZpl(): void {
    const zpl = this.data.items.map((item) => this.toZpl(item.reference)).join('\n');
    const blob = new Blob([zpl], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `labels-${this.data.items[0]?.reference ?? 'batch'}.zpl`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private toZpl(reference: string): string {
    const safe = reference.replace(/[^\x20-\x7E]/g, '');
    return `^XA\n^FO40,30^BY2\n^BCN,80,Y,N,N\n^FD${safe}^FS\n^FO40,150^A0N,28,28^FD${safe}^FS\n^XZ`;
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  private renderBarcodes(): void {
    const svgs = this.barcodeSvgs?.toArray() ?? [];
    const height = this.labelSize === '50x30' ? 40 : this.labelSize === '70x40' ? 56 : 64;
    this.data.items.forEach((item, index) => {
      const el = svgs[index]?.nativeElement;
      if (!el || !item.reference) {
        return;
      }
      try {
        JsBarcode(el, item.reference, {
          format: 'CODE128',
          width: 2,
          height,
          displayValue: true,
          fontSize: 12,
          margin: 6
        });
      } catch {
        /* invalid barcode value */
      }
    });
  }
}
