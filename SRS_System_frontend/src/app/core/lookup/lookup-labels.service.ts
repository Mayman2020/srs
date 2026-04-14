import { Injectable } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { LookupBundleDto, LookupItemDto } from '../api/api-types';
import { LookupService } from '../api/lookup.service';
import { I18nService } from '../i18n/i18n.service';
import { LookupCode, LookupTableKey, lookupTableKey } from './lookup-code';

/**
 * Resolves lookup `code` to display text from the shared lookup API.
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

  /** Load one lookup table through `GET /api/v1/lookups/{lookupCode}`. */
  loadTable(lookupCode: LookupCode): Observable<LookupItemDto[]> {
    return this.lookupApi.getByCode(lookupCode).pipe(
      tap((rows) => this.setTable(lookupTableKey(lookupCode), rows ?? []))
    );
  }

  private setTable(table: string, rows: LookupItemDto[]): void {
    this.byTable.set(table, new Map(rows.map((r) => [r.code, r])));
  }

  /** Populate label maps from a bundle (e.g. right after login or with create-transaction lookups). */
  hydrateFromBundle(b: LookupBundleDto): void {
    this.setTable(lookupTableKey(LookupCode.CorrespondenceType), b.correspondenceTypes);
    this.setTable(lookupTableKey(LookupCode.CorrespondenceStatus), b.correspondenceStatuses);
    this.setTable(lookupTableKey(LookupCode.Priority), b.priorities);
    this.setTable(lookupTableKey(LookupCode.Confidentiality), b.confidentialities);
    this.setTable(lookupTableKey(LookupCode.Classification), b.classifications ?? []);
    this.setTable(lookupTableKey(LookupCode.WorkflowActionType), b.workflowActionTypes);
    this.setTable(
      lookupTableKey(LookupCode.WorkflowHistoryEventType),
      b.workflowHistoryEventTypes ?? []
    );
    this.setTable(lookupTableKey(LookupCode.OrgVisualNodeStatus), b.orgVisualNodeStatuses ?? []);
  }

  orderedRows(table: LookupCode | LookupTableKey | string): LookupItemDto[] {
    const m = this.byTable.get(lookupTableKey(table));
    if (!m) return [];
    return [...m.values()].sort((a, b) => a.sortOrder - b.sortOrder);
  }

  displayName(row: LookupItemDto): string {
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  label(table: LookupCode | LookupTableKey | string, code: string | null | undefined): string {
    if (code == null || code === '') {
      return '\u2014';
    }
    const row = this.byTable.get(lookupTableKey(table))?.get(code);
    if (!row) {
      return code;
    }
    return this.displayName(row);
  }
}
