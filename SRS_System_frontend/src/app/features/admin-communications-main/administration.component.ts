import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { AdminConsoleApiService } from '../../core/api/admin-console-api.service';
import { RoleApiService } from '../../core/api/role-api.service';
import { DepartmentApiService } from '../../core/api/department-api.service';
import {
  DepartmentFlatDto,
  LookupItemDto,
  PermissionAdminDto,
  SystemIssueDto,
  UiScreenDto,
  UserDetailDto,
  UserListDto
} from '../../core/api/api-types';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

export type AdminTab = 'users' | 'permissions' | 'screens' | 'roles' | 'issues';

@Component({
  selector: 'app-administration',
  standalone: true,
  imports: [CommonModule, FormsModule, ScrollingModule, TranslatePipe, SrsDataTableComponent, SrsSortHeaderComponent],
  templateUrl: './administration.component.html',
  styleUrl: './administration.component.scss',
  animations: [srsTableRowEnter]
})
export class AdministrationComponent implements OnInit {
  activeTab: AdminTab = 'users';

  users: UserListDto[] = [];
  departments: DepartmentFlatDto[] = [];
  rolesLookup: LookupItemDto[] = [];

  permissions: PermissionAdminDto[] = [];
  screens: UiScreenDto[] = [];
  issues: SystemIssueDto[] = [];

  adminUsersQuery = '';
  adminUsersSortCol = 'name';
  adminUsersSortDir: SortDirection = 'asc';
  adminUsersPage = 1;
  adminUsersPageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  usersView: UserListDto[] = [];
  usersViewTotal = 0;

  permissionsLoading = false;
  permQuery = '';
  permSortCol = 'code';
  permSortDir: SortDirection = 'asc';
  permPage = 1;
  permPageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  permissionsView: PermissionAdminDto[] = [];
  permissionsTableTotal = 0;

  screensLoading = false;
  screenQuery = '';
  screenSortCol = 'code';
  screenSortDir: SortDirection = 'asc';
  screenPage = 1;
  screenPageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  screensView: UiScreenDto[] = [];
  screensTableTotal = 0;

  issueQuery = '';
  issueSortCol = 'when';
  issueSortDir: SortDirection = 'desc';
  issuePage = 1;
  issuePageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  issuesView: SystemIssueDto[] = [];
  issuesTableTotal = 0;

  /** Role matrix */
  matrixRoleId: number | null = null;
  matrixChecks: { id: number; code: string; label: string; checked: boolean }[] = [];
  matrixSaving = false;

  userModal: 'add' | 'edit' | 'view' | 'assign' | null = null;
  userForm: {
    id?: string;
    username: string;
    password: string;
    fullNameAr: string;
    fullNameEn: string;
    email: string;
    departmentId: number | null;
    active: boolean;
  } = this.emptyUserForm();
  assignRoleId: number | null = null;
  assignTargetUserId: string | null = null;

  permModal: 'add' | 'edit' | null = null;
  permForm = this.emptyPermForm();

  screenModal: 'add' | 'edit' | null = null;
  screenForm = this.emptyScreenForm();

  issueResolveId: number | null = null;
  issueResolveNote = '';

  loading = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private usersApi: UserDirectoryApiService,
    private adminApi: AdminConsoleApiService,
    private roleApi: RoleApiService,
    private deptApi: DepartmentApiService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const def = this.route.snapshot.data['defaultAdminTab'] as AdminTab | undefined;
    if (def && this.isTab(def)) {
      this.activeTab = def;
    }
    this.route.queryParamMap.subscribe((q) => {
      const t = q.get('tab') as AdminTab;
      if (t && this.isTab(t)) {
        this.activeTab = t;
      }
      this.refreshActiveTab();
    });
    subscribePageLoad({
      cdr: this.cdr,
      source: this.deptApi.list(),
      next: (rows) => (this.departments = rows ?? []),
      error: () => (this.departments = [])
    });
    subscribePageLoad({
      cdr: this.cdr,
      source: this.roleApi.list(),
      next: (r) => (this.rolesLookup = r ?? []),
      error: () => (this.rolesLookup = [])
    });
  }

  private isTab(t: string): t is AdminTab {
    return ['users', 'permissions', 'screens', 'roles', 'issues'].includes(t);
  }

  selectTab(tab: AdminTab): void {
    this.activeTab = tab;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
    this.refreshActiveTab();
  }

  private refreshActiveTab(): void {
    this.errorMsg = '';
    if (this.activeTab === 'users') {
      this.loadUsers();
    } else if (this.activeTab === 'permissions') {
      this.loadPermissions();
    } else if (this.activeTab === 'screens') {
      this.loadScreens();
    } else if (this.activeTab === 'roles') {
      subscribePageLoad({
        cdr: this.cdr,
        source: this.adminApi.listPermissions(),
        next: (p) => {
          this.permissions = p ?? [];
          this.rebuildPermissionsTab();
          if (!this.matrixRoleId && this.rolesLookup.length) {
            this.matrixRoleId = this.rolesLookup[0].id;
          }
          this.loadMatrix();
        },
        error: () => {
          this.permissions = [];
          this.permissionsView = [];
          this.permissionsTableTotal = 0;
          this.matrixChecks = [];
        }
      });
    } else if (this.activeTab === 'issues') {
      this.loadIssues();
    }
  }

  loadUsers(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.loading = v),
      source: this.usersApi.list(0, 500),
      next: (p) => {
        this.users = p.content ?? [];
        this.adminUsersPage = 1;
        this.rebuildUsersTab();
      },
      error: () => {
        this.users = [];
        this.usersView = [];
        this.usersViewTotal = 0;
      }
    });
  }

  loadPermissions(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.permissionsLoading = v),
      source: this.adminApi.listPermissions(),
      next: (r) => {
        this.permissions = r ?? [];
        this.permPage = 1;
        this.rebuildPermissionsTab();
      },
      error: () => {
        this.permissions = [];
        this.permissionsView = [];
        this.permissionsTableTotal = 0;
      }
    });
  }

  loadScreens(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.screensLoading = v),
      source: this.adminApi.listUiScreens(),
      next: (r) => {
        this.screens = r ?? [];
        this.screenPage = 1;
        this.rebuildScreensTab();
      },
      error: () => {
        this.screens = [];
        this.screensView = [];
        this.screensTableTotal = 0;
      }
    });
  }

  loadIssues(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.loading = v),
      source: this.adminApi.listSystemIssues(),
      next: (r) => {
        this.issues = r ?? [];
        this.issuePage = 1;
        this.rebuildIssuesTab();
      },
      error: () => {
        this.issues = [];
        this.issuesView = [];
        this.issuesTableTotal = 0;
      }
    });
  }

  loadMatrix(): void {
    if (this.matrixRoleId == null) {
      this.matrixChecks = [];
      return;
    }
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.matrixSaving = v),
      source: this.adminApi.getRolePermissionIds(this.matrixRoleId),
      next: (ids) => {
        const set = new Set(ids ?? []);
        this.matrixChecks = (this.permissions.length ? this.permissions : []).map((p) => ({
          id: p.id,
          code: p.code,
          label: this.labelPermission(p),
          checked: set.has(p.id)
        }));
      },
      error: () => {
        this.matrixChecks = [];
      }
    });
  }

  onMatrixRoleChange(): void {
    this.loadMatrix();
  }

  saveMatrix(): void {
    if (this.matrixRoleId == null) {
      return;
    }
    const ids = this.matrixChecks.filter((c) => c.checked).map((c) => c.id);
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.matrixSaving = v),
      source: this.adminApi.saveRolePermissionIds(this.matrixRoleId, ids),
      next: () => this.toastOk('admin.matrixSaved'),
      error: (e: unknown) => {
        const err = e as HttpErrorResponse & { userMessage?: string };
        this.errorMsg = err.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  openAddUser(): void {
    this.userForm = this.emptyUserForm();
    this.userModal = 'add';
  }

  openViewUser(u: UserListDto): void {
    this.usersApi.getOne(u.id).subscribe({
      next: (d) => this.patchUserFormFromDetail(d, true),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openEditUser(u: UserListDto): void {
    this.usersApi.getOne(u.id).subscribe({
      next: (d) => this.patchUserFormFromDetail(d, false),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openAssignRole(u: UserListDto): void {
    this.assignTargetUserId = u.id;
    this.assignRoleId = this.rolesLookup[0]?.id ?? null;
    this.userModal = 'assign';
  }

  saveUser(): void {
    if (this.userModal === 'add') {
      if (
        !this.userForm.username?.trim() ||
        !this.userForm.password ||
        !this.userForm.departmentId
      ) {
        this.errorMsg = this.i18n.instant('admin.validationUser');
        return;
      }
      this.usersApi
        .create({
          username: this.userForm.username.trim(),
          password: this.userForm.password,
          fullNameAr: this.userForm.fullNameAr.trim(),
          fullNameEn: this.userForm.fullNameEn.trim(),
          email: this.userForm.email.trim(),
          departmentId: this.userForm.departmentId!
        })
        .subscribe({
          next: () => {
            this.userModal = null;
            this.loadUsers();
            this.toastOk('admin.userCreated');
          },
          error: (e: HttpErrorResponse & { userMessage?: string }) => {
            this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
          }
        });
    } else if (this.userModal === 'edit' && this.userForm.id) {
      this.usersApi
        .update(this.userForm.id, {
          fullNameAr: this.userForm.fullNameAr.trim(),
          fullNameEn: this.userForm.fullNameEn.trim(),
          email: this.userForm.email.trim(),
          departmentId: this.userForm.departmentId!,
          active: this.userForm.active,
          password: this.userForm.password?.trim() || undefined
        })
        .subscribe({
          next: () => {
            this.userModal = null;
            this.loadUsers();
            this.toastOk('admin.userUpdated');
          },
          error: (e: HttpErrorResponse & { userMessage?: string }) => {
            this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
          }
        });
    }
  }

  confirmDeleteUser(u: UserListDto): void {
    if (!confirm(this.i18n.instant('admin.confirmDeleteUser'))) {
      return;
    }
    this.usersApi.delete(u.id).subscribe({
      next: () => {
        this.loadUsers();
        this.toastOk('admin.userDeleted');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  saveAssignRole(): void {
    if (!this.assignTargetUserId || this.assignRoleId == null) {
      return;
    }
    this.usersApi.assignRole(this.assignTargetUserId, this.assignRoleId).subscribe({
      next: () => {
        this.userModal = null;
        this.loadUsers();
        this.toastOk('admin.roleAssigned');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  closeUserModal(): void {
    this.userModal = null;
    this.assignTargetUserId = null;
  }

  openAddPerm(): void {
    this.permForm = this.emptyPermForm();
    this.permModal = 'add';
  }

  openEditPerm(p: PermissionAdminDto): void {
    this.permForm = {
      id: p.id,
      code: p.code,
      nameAr: p.nameAr,
      nameEn: p.nameEn,
      description: p.description ?? '',
      sortOrder: p.sortOrder,
      active: p.active
    };
    this.permModal = 'edit';
  }

  savePerm(): void {
    const body = {
      code: this.permForm.code.trim(),
      nameAr: this.permForm.nameAr.trim(),
      nameEn: this.permForm.nameEn.trim(),
      description: this.permForm.description?.trim() || null,
      sortOrder: Number(this.permForm.sortOrder) || 0,
      active: !!this.permForm.active
    };
    const obs =
      this.permModal === 'add'
        ? this.adminApi.createPermission(body)
        : this.adminApi.updatePermission(this.permForm.id!, body);
    obs.subscribe({
      next: () => {
        this.permModal = null;
        this.loadPermissions();
        if (this.activeTab === 'roles') {
          this.loadMatrix();
        }
        this.toastOk('admin.saved');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  deletePerm(p: PermissionAdminDto): void {
    if (!confirm(this.i18n.instant('admin.confirmDelete'))) {
      return;
    }
    this.adminApi.deletePermission(p.id).subscribe({
      next: () => {
        this.loadPermissions();
        this.loadMatrix();
        this.toastOk('admin.deleted');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  openAddScreen(): void {
    this.screenForm = this.emptyScreenForm();
    this.screenModal = 'add';
  }

  openEditScreen(s: UiScreenDto): void {
    this.screenForm = {
      id: s.id,
      code: s.code,
      routePath: s.routePath,
      nameAr: s.nameAr,
      nameEn: s.nameEn,
      description: s.description ?? '',
      sortOrder: s.sortOrder,
      active: s.active
    };
    this.screenModal = 'edit';
  }

  saveScreen(): void {
    const body = {
      code: this.screenForm.code.trim(),
      routePath: this.screenForm.routePath.trim(),
      nameAr: this.screenForm.nameAr.trim(),
      nameEn: this.screenForm.nameEn.trim(),
      description: this.screenForm.description?.trim() || null,
      sortOrder: Number(this.screenForm.sortOrder) || 0,
      active: !!this.screenForm.active
    };
    const obs =
      this.screenModal === 'add'
        ? this.adminApi.createUiScreen(body)
        : this.adminApi.updateUiScreen(this.screenForm.id!, body);
    obs.subscribe({
      next: () => {
        this.screenModal = null;
        this.loadScreens();
        this.toastOk('admin.saved');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  deleteScreen(s: UiScreenDto): void {
    if (!confirm(this.i18n.instant('admin.confirmDelete'))) {
      return;
    }
    this.adminApi.deleteUiScreen(s.id).subscribe({
      next: () => {
        this.loadScreens();
        this.toastOk('admin.deleted');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  openResolveIssue(i: SystemIssueDto): void {
    this.issueResolveId = i.id;
    this.issueResolveNote = '';
  }

  saveResolveIssue(): void {
    if (this.issueResolveId == null) {
      return;
    }
    this.adminApi.resolveSystemIssue(this.issueResolveId, this.issueResolveNote || null).subscribe({
      next: () => {
        this.issueResolveId = null;
        this.loadIssues();
        this.toastOk('admin.issueResolved');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  labelRoleCard(r: LookupItemDto): string {
    return this.i18n.currentLang() === 'ar'
      ? r.nameAr?.trim() || r.nameEn?.trim() || r.code
      : r.nameEn?.trim() || r.nameAr?.trim() || r.code;
  }

  labelPermission(p: PermissionAdminDto): string {
    return this.i18n.currentLang() === 'ar'
      ? p.nameAr?.trim() || p.nameEn?.trim() || p.code
      : p.nameEn?.trim() || p.nameAr?.trim() || p.code;
  }

  issueResolved(i: SystemIssueDto): boolean {
    return !!i.resolvedAt;
  }

  onAdminUsersQueryChange(): void {
    this.adminUsersPage = 1;
    this.rebuildUsersTab();
  }

  onAdminUserSort(ev: { columnId: string; direction: SortDirection }): void {
    this.adminUsersSortCol = ev.columnId;
    this.adminUsersSortDir = ev.direction;
    this.adminUsersPage = 1;
    this.rebuildUsersTab();
  }

  onAdminUserPage(p: number): void {
    this.adminUsersPage = p;
    this.rebuildUsersTab();
  }

  onAdminUserPageSize(n: number): void {
    this.adminUsersPageSize = n;
    this.adminUsersPage = 1;
    this.rebuildUsersTab();
  }

  onPermQueryChange(): void {
    this.permPage = 1;
    this.rebuildPermissionsTab();
  }

  onPermSort(ev: { columnId: string; direction: SortDirection }): void {
    this.permSortCol = ev.columnId;
    this.permSortDir = ev.direction;
    this.permPage = 1;
    this.rebuildPermissionsTab();
  }

  onPermPage(p: number): void {
    this.permPage = p;
    this.rebuildPermissionsTab();
  }

  onPermPageSize(n: number): void {
    this.permPageSize = n;
    this.permPage = 1;
    this.rebuildPermissionsTab();
  }

  onScreenQueryChange(): void {
    this.screenPage = 1;
    this.rebuildScreensTab();
  }

  onScreenSort(ev: { columnId: string; direction: SortDirection }): void {
    this.screenSortCol = ev.columnId;
    this.screenSortDir = ev.direction;
    this.screenPage = 1;
    this.rebuildScreensTab();
  }

  onScreenPage(p: number): void {
    this.screenPage = p;
    this.rebuildScreensTab();
  }

  onScreenPageSize(n: number): void {
    this.screenPageSize = n;
    this.screenPage = 1;
    this.rebuildScreensTab();
  }

  onIssueQueryChange(): void {
    this.issuePage = 1;
    this.rebuildIssuesTab();
  }

  onIssueSort(ev: { columnId: string; direction: SortDirection }): void {
    this.issueSortCol = ev.columnId;
    this.issueSortDir = ev.direction;
    this.issuePage = 1;
    this.rebuildIssuesTab();
  }

  onIssuePage(p: number): void {
    this.issuePage = p;
    this.rebuildIssuesTab();
  }

  onIssuePageSize(n: number): void {
    this.issuePageSize = n;
    this.issuePage = 1;
    this.rebuildIssuesTab();
  }

  trackByUserListId(_i: number, u: UserListDto): string {
    return u.id;
  }

  trackByPermId(_i: number, p: PermissionAdminDto): number {
    return p.id;
  }

  trackByScreenId(_i: number, s: UiScreenDto): number {
    return s.id;
  }

  trackByIssueId(_i: number, i: SystemIssueDto): number {
    return i.id;
  }

  trackByMatrixRow(_i: number, row: { id: number }): number {
    return row.id;
  }

  useMatrixVirtualScroll(): boolean {
    return this.matrixChecks.length > 48;
  }

  private rebuildUsersTab(): void {
    let rows = this.filterAdminUsers([...this.users]);
    rows = this.sortAdminUsers(rows);
    const r = srsClientPaginate(rows, this.adminUsersPage, this.adminUsersPageSize);
    this.adminUsersPage = r.page;
    this.usersViewTotal = r.total;
    this.usersView = r.pageRows;
  }

  private filterAdminUsers(rows: UserListDto[]): UserListDto[] {
    const q = this.adminUsersQuery.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter((u) => {
      const name = (u.fullNameAr || u.fullNameEn || u.username || '').toLowerCase();
      const un = (u.username || '').toLowerCase();
      const em = (u.email || '').toLowerCase();
      const dep = (u.departmentCode || '').toLowerCase();
      return name.includes(q) || un.includes(q) || em.includes(q) || dep.includes(q);
    });
  }

  private sortAdminUsers(rows: UserListDto[]): UserListDto[] {
    const col = this.adminUsersSortCol;
    const dir = this.adminUsersSortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'name': {
          const na = (a.fullNameAr || a.fullNameEn || a.username || '').toLowerCase();
          const nb = (b.fullNameAr || b.fullNameEn || b.username || '').toLowerCase();
          return compareSortValues(na, nb, dir);
        }
        case 'username':
          return compareSortValues(a.username, b.username, dir);
        case 'email':
          return compareSortValues(a.email ?? '', b.email ?? '', dir);
        case 'dept':
          return compareSortValues(a.departmentCode ?? '', b.departmentCode ?? '', dir);
        case 'status':
          return compareSortValues(a.active ? 1 : 0, b.active ? 1 : 0, dir);
        default:
          return 0;
      }
    });
  }

  private rebuildPermissionsTab(): void {
    let rows = this.filterPermissions([...this.permissions]);
    rows = this.sortPermissions(rows);
    const r = srsClientPaginate(rows, this.permPage, this.permPageSize);
    this.permPage = r.page;
    this.permissionsTableTotal = r.total;
    this.permissionsView = r.pageRows;
  }

  private filterPermissions(rows: PermissionAdminDto[]): PermissionAdminDto[] {
    const q = this.permQuery.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter(
      (p) =>
        (p.code || '').toLowerCase().includes(q) ||
        (p.nameAr || '').toLowerCase().includes(q) ||
        (p.nameEn || '').toLowerCase().includes(q)
    );
  }

  private sortPermissions(rows: PermissionAdminDto[]): PermissionAdminDto[] {
    const col = this.permSortCol;
    const dir = this.permSortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'code':
          return compareSortValues(a.code, b.code, dir);
        case 'nameAr':
          return compareSortValues(a.nameAr, b.nameAr, dir);
        case 'nameEn':
          return compareSortValues(a.nameEn, b.nameEn, dir);
        case 'sortOrder':
          return compareSortValues(a.sortOrder, b.sortOrder, dir);
        case 'active':
          return compareSortValues(a.active ? 1 : 0, b.active ? 1 : 0, dir);
        default:
          return 0;
      }
    });
  }

  private rebuildScreensTab(): void {
    let rows = this.filterScreens([...this.screens]);
    rows = this.sortScreens(rows);
    const r = srsClientPaginate(rows, this.screenPage, this.screenPageSize);
    this.screenPage = r.page;
    this.screensTableTotal = r.total;
    this.screensView = r.pageRows;
  }

  private filterScreens(rows: UiScreenDto[]): UiScreenDto[] {
    const q = this.screenQuery.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter(
      (s) =>
        (s.code || '').toLowerCase().includes(q) ||
        (s.routePath || '').toLowerCase().includes(q) ||
        (s.nameAr || '').toLowerCase().includes(q) ||
        (s.nameEn || '').toLowerCase().includes(q)
    );
  }

  private sortScreens(rows: UiScreenDto[]): UiScreenDto[] {
    const col = this.screenSortCol;
    const dir = this.screenSortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'code':
          return compareSortValues(a.code, b.code, dir);
        case 'route':
          return compareSortValues(a.routePath, b.routePath, dir);
        case 'nameAr':
          return compareSortValues(a.nameAr, b.nameAr, dir);
        case 'nameEn':
          return compareSortValues(a.nameEn, b.nameEn, dir);
        default:
          return 0;
      }
    });
  }

  private rebuildIssuesTab(): void {
    let rows = this.filterIssues([...this.issues]);
    rows = this.sortIssues(rows);
    const r = srsClientPaginate(rows, this.issuePage, this.issuePageSize);
    this.issuePage = r.page;
    this.issuesTableTotal = r.total;
    this.issuesView = r.pageRows;
  }

  private filterIssues(rows: SystemIssueDto[]): SystemIssueDto[] {
    const q = this.issueQuery.trim().toLowerCase();
    if (!q) {
      return rows;
    }
    return rows.filter(
      (i) =>
        (i.message || '').toLowerCase().includes(q) ||
        (i.pageUrl || '').toLowerCase().includes(q) ||
        (i.severity || '').toLowerCase().includes(q)
    );
  }

  private sortIssues(rows: SystemIssueDto[]): SystemIssueDto[] {
    const col = this.issueSortCol;
    const dir = this.issueSortDir;
    return rows.sort((a, b) => {
      switch (col) {
        case 'when': {
          const ta = new Date(a.createdAt).getTime();
          const tb = new Date(b.createdAt).getTime();
          return compareSortValues(
            Number.isNaN(ta) ? 0 : ta,
            Number.isNaN(tb) ? 0 : tb,
            dir
          );
        }
        case 'severity':
          return compareSortValues(a.severity, b.severity, dir);
        case 'message':
          return compareSortValues(a.message, b.message, dir);
        case 'url':
          return compareSortValues(a.pageUrl ?? '', b.pageUrl ?? '', dir);
        case 'http':
          return compareSortValues(a.httpStatus ?? -1, b.httpStatus ?? -1, dir);
        case 'resolved':
          return compareSortValues(this.issueResolved(a) ? 1 : 0, this.issueResolved(b) ? 1 : 0, dir);
        default:
          return 0;
      }
    });
  }

  private emptyUserForm() {
    return {
      username: '',
      password: '',
      fullNameAr: '',
      fullNameEn: '',
      email: '',
      departmentId: null as number | null,
      active: true
    };
  }

  private patchUserFormFromDetail(d: UserDetailDto, viewOnly: boolean): void {
    this.userForm = {
      id: d.id,
      username: d.username,
      password: '',
      fullNameAr: d.fullNameAr,
      fullNameEn: d.fullNameEn,
      email: d.email,
      departmentId: d.departmentId,
      active: d.active
    };
    this.userModal = viewOnly ? 'view' : 'edit';
  }

  private emptyPermForm() {
    return {
      id: undefined as number | undefined,
      code: '',
      nameAr: '',
      nameEn: '',
      description: '',
      sortOrder: 100,
      active: true
    };
  }

  private emptyScreenForm() {
    return {
      id: undefined as number | undefined,
      code: '',
      routePath: '',
      nameAr: '',
      nameEn: '',
      description: '',
      sortOrder: 100,
      active: true
    };
  }

  private toastOk(key: string): void {
    this.errorMsg = '';
    this.successMsg = this.i18n.instant(key);
    setTimeout(() => {
      this.successMsg = '';
    }, 4000);
  }
}
