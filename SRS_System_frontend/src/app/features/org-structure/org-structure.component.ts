import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { OrganizationApiService } from '../../core/api/organization-api.service';
import { DepartmentFlatDto, OrganizationFlatDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-org-structure',
  standalone: true,
  imports: [CommonModule, TranslatePipe, RouterLink],
  templateUrl: './org-structure.component.html',
  styleUrl: './org-structure.component.css'
})
export class OrgStructureComponent implements OnInit {
  tab: 'dept' | 'org' = 'dept';
  departments: DepartmentFlatDto[] = [];
  organizations: OrganizationFlatDto[] = [];
  loading = true;

  constructor(
    private readonly deptApi: DepartmentApiService,
    private readonly orgApi: OrganizationApiService,
    private readonly cdr: ChangeDetectorRef
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
}
