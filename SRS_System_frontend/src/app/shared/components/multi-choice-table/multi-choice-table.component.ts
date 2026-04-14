import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

export type MultiChoiceId = number | string;

export interface MultiChoiceOption {
  id: MultiChoiceId;
  label: string;
  subtitle?: string | null;
  code?: string | null;
}

@Component({
  selector: 'srs-multi-choice-table',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './multi-choice-table.component.html',
  styleUrl: './multi-choice-table.component.scss'
})
export class MultiChoiceTableComponent {
  @Input() options: readonly MultiChoiceOption[] = [];
  @Input() selectedIds: readonly MultiChoiceId[] = [];
  @Output() selectedIdsChange = new EventEmitter<MultiChoiceId[]>();

  @Input() labelKey = '';
  @Input() placeholderKey = 'genericSelect.placeholder';
  @Input() selectedTitleKey = 'admin.assignRoleSelected';
  @Input() emptyKey = 'common.empty';
  @Input() nameHeaderKey = 'admin.colName';
  @Input() codeHeaderKey = 'admin.colCode';
  @Input() actionHeaderKey = 'admin.colActions';
  @Input() disabled = false;
  @Input() showCode = true;
  @Input() required = false;

  query = '';
  open = false;

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open = false;
    }
  }

  get selectedOptions(): MultiChoiceOption[] {
    return this.selectedIds.map((id) => this.findOption(id) ?? this.fallbackOption(id));
  }

  get filteredOptions(): MultiChoiceOption[] {
    const q = this.query.trim().toLowerCase();
    return this.options
      .filter((option) => !this.isSelected(option.id))
      .filter((option) => {
        if (!q) {
          return true;
        }
        const haystack = [option.label, option.subtitle ?? '', option.code ?? '']
          .join(' ')
          .toLowerCase();
        return haystack.includes(q);
      });
  }

  add(option: MultiChoiceOption): void {
    if (this.disabled || this.isSelected(option.id)) {
      return;
    }
    this.emitSelection([...this.selectedIds, option.id]);
    this.query = '';
    this.open = false;
  }

  remove(id: MultiChoiceId): void {
    if (this.disabled) {
      return;
    }
    this.emitSelection(this.selectedIds.filter((selectedId) => !this.sameId(selectedId, id)));
  }

  clearQuery(): void {
    this.query = '';
    this.open = true;
  }

  trackById(_index: number, option: MultiChoiceOption): string {
    return String(option.id);
  }

  private emitSelection(ids: readonly MultiChoiceId[]): void {
    const seen = new Set<string>();
    const unique = ids.filter((id) => {
      const key = String(id);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
    this.selectedIdsChange.emit(unique);
  }

  private isSelected(id: MultiChoiceId): boolean {
    return this.selectedIds.some((selectedId) => this.sameId(selectedId, id));
  }

  private findOption(id: MultiChoiceId): MultiChoiceOption | undefined {
    return this.options.find((option) => this.sameId(option.id, id));
  }

  private fallbackOption(id: MultiChoiceId): MultiChoiceOption {
    return {
      id,
      label: `#${id}`,
      code: String(id)
    };
  }

  private sameId(a: MultiChoiceId, b: MultiChoiceId): boolean {
    return String(a) === String(b);
  }
}
