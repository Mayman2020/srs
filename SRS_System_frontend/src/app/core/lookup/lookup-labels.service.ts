import { Injectable } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { LookupBundleDto, LookupItemDto } from '../api/api-types';
import { LookupService } from '../api/lookup.service';
import { I18nService } from '../i18n/i18n.service';

/**
 * Resolves lookup `code` → display text from `/api/v1/lookups` (`name_ar` / `name_en`).
 * Business/domain labels must use this service (or API fields), not i18n JSON.
 */
@Injectable({ providedIn: 'root' })
export class LookupLabelsService {
  private readonly byTable = new Map<string, Map<string, LookupItemDto>>();

  constructor(
    private readonly lookupApi: LookupService,
    private readonly i18n: I18nService
  ) {}

  /** Load and cache the lookup bundle (idempotent; uses shared HTTP cache). */
  load(): Observable<void> {
    return this.lookupApi.getBundle().pipe(
      tap((b) => {
        this.hydrateFromBundle(b);
      }),
      map(() => undefined)
    );
  }

  private setTable(table: string, rows: LookupItemDto[]): void {
    this.byTable.set(table, new Map(rows.map((r) => [r.code, r])));
  }

  /** Populate label maps from a bundle (e.g. right after login or with create-transaction lookups). */
  hydrateFromBundle(b: LookupBundleDto): void {
    this.setTable('correspondenceType', b.correspondenceTypes);
    this.setTable('correspondenceStatus', b.correspondenceStatuses);
    this.setTable('priority', b.priorities);
    this.setTable('confidentiality', b.confidentialities);
    this.setTable('classification', b.classifications ?? []);
    this.setTable('workflowActionType', b.workflowActionTypes);
    this.setTable('workflowHistoryEventType', b.workflowHistoryEventTypes ?? []);
    this.setTable('orgVisualNodeStatus', b.orgVisualNodeStatuses ?? []);
  }

  orderedRows(table: string): LookupItemDto[] {
    const m = this.byTable.get(table);
    if (!m) return [];
    return [...m.values()].sort((a, b) => a.sortOrder - b.sortOrder);
  }

  displayName(row: LookupItemDto): string {
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  label(table: string, code: string | null | undefined): string {
    if (code == null || code === '') {
      return '\u2014';
    }
    const row = this.byTable.get(table)?.get(code);
    if (!row) {
      return code;
    }
    return this.displayName(row);
  }
}
