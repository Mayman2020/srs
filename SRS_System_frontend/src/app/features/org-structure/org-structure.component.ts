import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { OrganizationApiService } from '../../core/api/organization-api.service';
import { DepartmentFlatDto, OrganizationFlatDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { RouterLink } from '@angular/router';
import { matchesTableSearch } from '../../core/util/table-text-filter';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';

@Component({
  selector: 'app-org-structure',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    RouterLink,
    SrsDataTableComponent
  ],
  templateUrl: './org-structure.component.html',
  styleUrl: './org-structure.component.css'
})
export class OrgStructureComponent implements OnInit {
  tab: 'dept' | 'org' = 'dept';
  departments: DepartmentFlatDto[] = [];
  organizations: OrganizationFlatDto[] = [];
  loading = true;

  /** Search across all visible columns (client-side). */
  searchDept = '';
  searchOrg = '';

  constructor(
    private readonly deptApi: DepartmentApiService,
    private readonly orgApi: OrganizationApiService,
    private readonly cdr: ChangeDetectorRef,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: forkJoin({
        departments: this.deptApi.list(),
        organizations: this.orgApi.list()
      }),
      setLoading: (loading) => (this.loading = loading),
      next: ({ departments, organizations }) => {
        this.departments = departments ?? [];
        this.organizations = organizations ?? [];
      },
      error: () => {
        this.departments = [];
        this.organizations = [];
      }
    });
  }

  treeRowsDept(): DepartmentFlatDto[] {
    return [...this.departments].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  }

  filteredDepartments(): DepartmentFlatDto[] {
    const rows = this.treeRowsDept();
    return rows.filter((d) =>
      matchesTableSearch(this.searchDept, [d.id, d.parentId, d.code, d.nameAr, d.nameEn])
    );
  }

  sortedOrganizations(): OrganizationFlatDto[] {
    return [...this.organizations].sort((a, b) => a.id - b.id);
  }

  filteredOrganizations(): OrganizationFlatDto[] {
    const rows = this.sortedOrganizations();
    const yes = this.i18n.instant('common.yes');
    const no = this.i18n.instant('common.no');
    return rows.filter((o) =>
      matchesTableSearch(this.searchOrg, [
        o.id,
        o.parentId,
        o.code,
        o.nameAr,
        o.nameEn,
        o.external,
        yes,
        no
      ])
    );
  }
}
