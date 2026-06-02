import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import {
  OrganizationalUnitLevel,
  OrgRoutingApiService
} from '../../core/api/org-routing-api.service';

/**
 * Read-only admin grid showing the Q / L / K / S organizational hierarchy levels.
 *
 * The level catalog is seeded by Flyway V5. CRUD is intentionally not exposed yet — the
 * levels are part of the routing contract; the next iteration introduces an admin-only
 * edit dialog for {@code rankOrder} (active flag toggle is already supported by the
 * backend lookup machinery).
 */
@Component({
  selector: 'app-org-levels',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  template: `
    <section class="org-levels">
      <header class="org-levels__header">
        <h1>{{ 'orgLevels.pageTitle' | t }}</h1>
        <p class="org-levels__subtitle">{{ 'orgLevels.pageSubtitle' | t }}</p>
      </header>

      <ng-container *ngIf="!loading; else loadingTpl">
        <p class="org-levels__error" *ngIf="errorKey">{{ errorKey | t }}</p>

        <table class="org-levels__table" *ngIf="!errorKey">
          <thead>
            <tr>
              <th>{{ 'orgLevels.col.code' | t }}</th>
              <th>{{ 'orgLevels.col.name' | t }}</th>
              <th>{{ 'orgLevels.col.rank' | t }}</th>
              <th>{{ 'orgLevels.col.active' | t }}</th>
              <th>{{ 'orgLevels.col.description' | t }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let l of levels">
              <td class="org-levels__code">{{ l.code }}</td>
              <td>{{ nameOf(l) }}</td>
              <td>{{ l.rankOrder }}</td>
              <td>{{ (l.active ? 'common.yes' : 'common.no') | t }}</td>
              <td class="org-levels__desc">{{ l.description }}</td>
            </tr>
            <tr *ngIf="!levels.length">
              <td colspan="5" class="org-levels__empty">{{ 'common.noResults' | t }}</td>
            </tr>
          </tbody>
        </table>
      </ng-container>

      <ng-template #loadingTpl>
        <p>{{ 'common.loading' | t }}</p>
      </ng-template>
    </section>
  `,
  styles: [
    `
      .org-levels { padding: 1.5rem; }
      .org-levels__header { margin-bottom: 1rem; }
      .org-levels__subtitle { color: var(--text-muted, #6b7280); margin: 0.25rem 0 0; }
      .org-levels__error { color: var(--danger, #b91c1c); }
      .org-levels__table { width: 100%; border-collapse: collapse; }
      .org-levels__table th,
      .org-levels__table td { padding: 0.5rem 0.75rem; text-align: start; border-bottom: 1px solid var(--border, #e5e7eb); }
      .org-levels__code { font-family: 'JetBrains Mono', ui-monospace, monospace; font-weight: 600; }
      .org-levels__desc { color: var(--text-muted, #6b7280); }
      .org-levels__empty { text-align: center; padding: 1.5rem; color: var(--text-muted, #6b7280); }
    `
  ]
})
export class OrgLevelsComponent implements OnInit {
  private readonly api = inject(OrgRoutingApiService);
  private readonly i18n = inject(I18nService);

  loading = true;
  errorKey: string | null = null;
  levels: OrganizationalUnitLevel[] = [];

  ngOnInit(): void {
    this.api.listLevels().subscribe({
      next: (list) => {
        this.levels = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.errorKey = 'orgLevels.loadFailed';
        this.loading = false;
      }
    });
  }

  nameOf(level: OrganizationalUnitLevel): string {
    return this.i18n.currentLang() === 'en' ? level.nameEn : level.nameAr;
  }
}
