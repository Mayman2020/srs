import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { OrganizationApiService, UpsertOrganizationRequestDto } from '../../core/api/organization-api.service';
import { DepartmentFlatDto, OrganizationFlatDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { RouterLink } from '@angular/router';
import { matchesTableSearch } from '../../core/util/table-text-filter';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';
import {
  OrgStructureOrganizationDialogComponent,
  OrgStructureOrganizationDialogData
} from './org-structure-organization-dialog.component';

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

  private readonly deptApi = inject(DepartmentApiService);
  private readonly orgApi = inject(OrganizationApiService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly i18n = inject(I18nService);
  private readonly matDialog = inject(MatDialog);
  private readonly dialog = inject(DialogService);
  private readonly toast = inject(NotificationService);

  constructor() {}

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

  reloadOrganizations(): void {
    this.orgApi.list().subscribe({
      next: (rows) => (this.organizations = rows ?? []),
      error: () => (this.organizations = [])
    });
  }

  openOrgCreate(): void {
    this.openOrgDialog('create');
  }

  openOrgEdit(org: OrganizationFlatDto): void {
    this.openOrgDialog('edit', org);
  }

  deleteOrg(org: OrganizationFlatDto): void {
    const name = this.i18n.currentLang() === 'en' ? org.nameEn : org.nameAr;
    this.dialog
      .openConfirm({
        titleKey: 'orgCrud.deleteTitle',
        messageKey: 'orgCrud.deleteMessage',
        params: { name },
        confirmButton: { labelKey: 'common.delete', color: 'warn' },
        cancelButton: { labelKey: 'common.cancel' }
      })
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.orgApi.delete(org.id).subscribe({
          next: () => {
            this.toast.success('orgCrud.deleted');
            this.reloadOrganizations();
          },
          error: () => this.toast.error('orgCrud.deleteFailed')
        });
      });
  }

  private openOrgDialog(mode: 'create' | 'edit', organization?: OrganizationFlatDto): void {
    const ref = this.matDialog.open(OrgStructureOrganizationDialogComponent, {
      width: 'min(480px, 94vw)',
      data: {
        mode,
        organization,
        organizations: this.organizations
      } satisfies OrgStructureOrganizationDialogData
    });
    ref.afterClosed().subscribe((body: UpsertOrganizationRequestDto | undefined) => {
      if (!body) {
        return;
      }
      const req =
        mode === 'edit' && organization
          ? this.orgApi.update(organization.id, body)
          : this.orgApi.create(body);
      req.subscribe({
        next: () => {
          this.toast.success(mode === 'edit' ? 'orgCrud.updated' : 'orgCrud.created');
          this.reloadOrganizations();
        },
        error: () => this.toast.error('orgCrud.saveFailed')
      });
    });
  }
}
