import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { AdminConsoleApiService } from '../../core/api/admin-console-api.service';
import { LookupItemDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ReferenceDataCacheService } from '../../core/cache/reference-data-cache.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { SrsEmptyStateComponent } from '../../shared/data-table/srs-empty-state.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';

export interface RoleCardVm {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  description: string;
  usersCount: number;
  permissions: { name: string; enabled: boolean }[];
}

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule,
    TranslatePipe,
    RouterLink,
    ScrollingModule,
    SrsDataTableComponent,
    SrsSortHeaderComponent,
    SrsEmptyStateComponent
  ],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
  animations: [srsTableRowEnter]
})
export class RolesComponent implements OnInit {
  roles: RoleCardVm[] = [];
  pageRoles: RoleCardVm[] = [];
  rolesTotal = 0;
  page = 1;
  pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  sortColumn = 'name';
  sortDir: SortDirection = 'asc';

  selectedRole: RoleCardVm | null = null;
  showPermissionsModal = false;
  permsLoadError = false;
  loading = false;

  constructor(
    private adminApi: AdminConsoleApiService,
    private i18n: I18nService,
    private refCache: ReferenceDataCacheService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.loading = v),
      source: this.refCache.roles(),
      next: (items) => {
        this.roles = (items ?? []).map((r) => this.mapRole(r));
        this.page = 1;
        this.rebuildRoleTable();
      },
      error: () => {
        this.roles = [];
        this.pageRoles = [];
        this.rolesTotal = 0;
      }
    });
  }

  private mapRole(r: LookupItemDto): RoleCardVm {
    return {
      id: r.id,
      code: r.code,
      nameAr: r.nameAr?.trim() || r.nameEn?.trim() || r.code,
      nameEn: r.nameEn?.trim() || r.nameAr?.trim() || r.code,
      description: r.code,
      usersCount: 0,
      permissions: []
    };
  }

  roleName(role: RoleCardVm): string {
    return this.i18n.currentLang() === 'ar'
      ? role.nameAr || role.nameEn || role.code
      : role.nameEn || role.nameAr || role.code;
  }

  roleDescription(role: RoleCardVm): string {
    return role.description || role.code;
  }

  onRoleSort(ev: { columnId: string; direction: SortDirection }): void {
    this.sortColumn = ev.columnId;
    this.sortDir = ev.direction;
    this.page = 1;
    this.rebuildRoleTable();
  }

  onRolePage(p: number): void {
    this.page = p;
    this.rebuildRoleTable();
  }

  onRolePageSize(n: number): void {
    this.pageSize = n;
    this.page = 1;
    this.rebuildRoleTable();
  }

  trackByRoleId(_i: number, r: RoleCardVm): number {
    return r.id;
  }

  trackByPermName(_i: number, p: { name: string }): string {
    return p.name;
  }

  private rebuildRoleTable(): void {
    const sorted = this.sortRoles([...this.roles]);
    const r = srsClientPaginate(sorted, this.page, this.pageSize);
    this.page = r.page;
    this.rolesTotal = r.total;
    this.pageRoles = r.pageRows;
  }

  private sortRoles(rows: RoleCardVm[]): RoleCardVm[] {
    const col = this.sortColumn;
    const dir = this.sortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'name':
          return compareSortValues(this.roleName(a), this.roleName(b), dir);
        case 'code':
          return compareSortValues(a.code, b.code, dir);
        case 'users':
          return compareSortValues(a.usersCount, b.usersCount, dir);
        default:
          return 0;
      }
    });
  }

  viewPermissions(role: RoleCardVm): void {
    const rid = Number(role.id);
    if (!Number.isFinite(rid)) {
      return;
    }
    this.permsLoadError = false;
    subscribePageLoad({
      cdr: this.cdr,
      source: forkJoin({
        perms: this.adminApi.listPermissions(),
        ids: this.adminApi.getRolePermissionIds(rid)
      }),
      next: ({ perms, ids }) => {
        const set = new Set(ids ?? []);
        const rows = (perms ?? []).map((p) => ({
          name: this.i18n.currentLang() === 'en' ? p.nameEn : p.nameAr,
          enabled: set.has(p.id)
        }));
        this.selectedRole = { ...role, permissions: rows };
        this.showPermissionsModal = true;
      },
      error: () => {
        this.permsLoadError = true;
        this.selectedRole = { ...role, permissions: [] };
        this.showPermissionsModal = true;
      }
    });
  }

  closeModal(): void {
    this.showPermissionsModal = false;
    this.selectedRole = null;
    this.permsLoadError = false;
  }

  useVirtualScrollForPerms(): boolean {
    return (this.selectedRole?.permissions?.length ?? 0) > 40;
  }
}
