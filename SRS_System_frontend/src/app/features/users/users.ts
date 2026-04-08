import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { DepartmentApiService } from '../../core/api/department-api.service';
import { RoleApiService } from '../../core/api/role-api.service';
import {
  DepartmentFlatDto,
  LookupItemDto,
  UserDetailDto,
  UserListDto
} from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { SrsSortHeaderComponent } from '../../shared/data-table/srs-sort-header.component';
import { srsTableRowEnter } from '../../shared/data-table/srs-table.animations';
import { compareSortValues, type SortDirection } from '../../shared/data-table/table-sort.util';
import { SRS_TABLE_DEFAULT_PAGE_SIZE } from '../../shared/data-table/srs-table-defaults';
import { srsClientPaginate } from '../../shared/data-table/srs-client-pagination.util';
import { ErpPageToolbarComponent } from '../../shared/erp/erp-page-toolbar.component';

/** Standalone users page (original app layout) backed by `/api/v1/users`. */
@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    SrsDataTableComponent,
    SrsSortHeaderComponent,
    ErpPageToolbarComponent
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
  animations: [srsTableRowEnter]
})
export class UsersComponent implements OnInit {
  users: UserListDto[] = [];
  filteredUsers: UserListDto[] = [];
  pageUsers: UserListDto[] = [];
  departments: DepartmentFlatDto[] = [];
  rolesLookup: LookupItemDto[] = [];

  searchQuery = '';
  filterStatus: 'all' | 'active' | 'suspended' = 'all';

  page = 1;
  pageSize = SRS_TABLE_DEFAULT_PAGE_SIZE;
  sortColumn = 'name';
  sortDir: SortDirection = 'asc';

  loading = false;
  errorMsg = '';
  successMsg = '';

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
    roleIds?: number[];
  } = this.emptyUserForm();

  assignRoleId: number | null = null;
  assignTargetUserId: string | null = null;

  constructor(
    private userApi: UserDirectoryApiService,
    private deptApi: DepartmentApiService,
    private roleApi: RoleApiService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: forkJoin({
        depts: this.deptApi.list(),
        roles: this.roleApi.list()
      }),
      next: ({ depts, roles }) => {
        this.departments = depts ?? [];
        this.rolesLookup = roles ?? [];
      },
      error: () => {
        this.departments = [];
        this.rolesLookup = [];
      }
    });
    this.loadUsers();
  }

  loadUsers(): void {
    this.errorMsg = '';
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (v) => (this.loading = v),
      source: this.userApi.list(0, 500),
      next: (page) => {
        this.users = page.content ?? [];
        this.applyFilters();
      },
      error: () => {
        this.users = [];
        this.filteredUsers = [];
        this.pageUsers = [];
      }
    });
  }

  displayName(u: UserListDto): string {
    return u.fullNameAr?.trim() || u.fullNameEn?.trim() || u.username;
  }

  applyFilters(): void {
    let results = [...this.users];
    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      results = results.filter(
        (u) =>
          this.displayName(u).toLowerCase().includes(q) ||
          u.username.toLowerCase().includes(q) ||
          (u.email ?? '').toLowerCase().includes(q) ||
          (u.departmentCode ?? '').toLowerCase().includes(q)
      );
    }
    if (this.filterStatus === 'active') {
      results = results.filter((u) => u.active);
    } else if (this.filterStatus === 'suspended') {
      results = results.filter((u) => !u.active);
    }
    this.filteredUsers = results;
    this.page = 1;
    this.applyUserTableSlice();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onUserSort(ev: { columnId: string; direction: SortDirection }): void {
    this.sortColumn = ev.columnId;
    this.sortDir = ev.direction;
    this.page = 1;
    this.applyUserTableSlice();
  }

  onUserPage(p: number): void {
    this.page = p;
    this.applyUserTableSlice();
  }

  onUserPageSize(n: number): void {
    this.pageSize = n;
    this.page = 1;
    this.applyUserTableSlice();
  }

  trackByUserId(_i: number, u: UserListDto): string {
    return u.id;
  }

  private applyUserTableSlice(): void {
    const sorted = this.sortUsers(this.filteredUsers);
    const r = srsClientPaginate(sorted, this.page, this.pageSize);
    this.page = r.page;
    this.pageUsers = r.pageRows;
  }

  private sortUsers(rows: UserListDto[]): UserListDto[] {
    const col = this.sortColumn;
    const dir = this.sortDir;
    return [...rows].sort((a, b) => {
      switch (col) {
        case 'name':
          return compareSortValues(this.displayName(a), this.displayName(b), dir);
        case 'username':
          return compareSortValues(a.username, b.username, dir);
        case 'role':
          return compareSortValues(this.formatRoleLabels(a.roleIds), this.formatRoleLabels(b.roleIds), dir);
        case 'dept':
          return compareSortValues(a.departmentCode ?? '', b.departmentCode ?? '', dir);
        case 'email':
          return compareSortValues(a.email ?? '', b.email ?? '', dir);
        case 'phone':
          return compareSortValues(a.username, b.username, dir);
        case 'status':
          return compareSortValues(a.active ? 1 : 0, b.active ? 1 : 0, dir);
        default:
          return 0;
      }
    });
  }

  statusLabel(active: boolean): string {
    return this.i18n.instant(active ? 'users.statusActive' : 'users.statusSuspended');
  }

  labelRoleCard(r: LookupItemDto): string {
    return this.i18n.currentLang() === 'ar'
      ? r.nameAr?.trim() || r.nameEn?.trim() || r.code
      : r.nameEn?.trim() || r.nameAr?.trim() || r.code;
  }

  formatRoleLabels(roleIds: number[] | undefined): string {
    if (!roleIds?.length) {
      return this.i18n.instant('common.roleNA');
    }
    return roleIds
      .map((id) => this.rolesLookup.find((role) => role.id === id))
      .filter((role): role is LookupItemDto => !!role)
      .map((role) => this.labelRoleCard(role))
      .join(', ');
  }

  openAddModal(): void {
    this.userForm = this.emptyUserForm();
    this.userModal = 'add';
  }

  openViewModal(u: UserListDto): void {
    this.userApi.getOne(u.id).subscribe({
      next: (d) => this.patchUserFormFromDetail(d, true),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openEditModal(u: UserListDto): void {
    this.userApi.getOne(u.id).subscribe({
      next: (d) => this.patchUserFormFromDetail(d, false),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openAssignModal(u: UserListDto): void {
    this.assignTargetUserId = u.id;
    this.assignRoleId = this.rolesLookup[0]?.id ?? null;
    this.userModal = 'assign';
  }

  closeUserModal(): void {
    this.userModal = null;
    this.assignTargetUserId = null;
  }

  saveUser(): void {
    if (this.userModal === 'add') {
      if (!this.userForm.username?.trim() || !this.userForm.password || !this.userForm.departmentId) {
        this.errorMsg = this.i18n.instant('admin.validationUser');
        return;
      }
      this.userApi
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
      this.userApi
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

  saveAssignRole(): void {
    if (!this.assignTargetUserId || this.assignRoleId == null) {
      return;
    }
    this.userApi.assignRole(this.assignTargetUserId, this.assignRoleId).subscribe({
      next: () => {
        this.userModal = null;
        this.assignTargetUserId = null;
        this.loadUsers();
        this.toastOk('admin.roleAssigned');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  deleteUser(u: UserListDto): void {
    if (!confirm(this.i18n.instant('admin.confirmDeleteUser'))) {
      return;
    }
    this.userApi.delete(u.id).subscribe({
      next: () => {
        this.loadUsers();
        this.toastOk('admin.userDeleted');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  toggleUserStatus(u: UserListDto): void {
    this.userApi.getOne(u.id).subscribe({
      next: (d) => {
        if (d.departmentId == null) {
          this.errorMsg = this.i18n.instant('admin.validationUser');
          return;
        }
        this.userApi
          .update(u.id, {
            fullNameAr: d.fullNameAr,
            fullNameEn: d.fullNameEn,
            email: d.email,
            departmentId: d.departmentId,
            active: !d.active
          })
          .subscribe({
            next: () => {
              this.loadUsers();
              this.toastOk('admin.userUpdated');
            },
            error: (e: HttpErrorResponse & { userMessage?: string }) => {
              this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
            }
          });
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
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
      active: true,
      roleIds: [] as number[]
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
      active: d.active,
      roleIds: d.roleIds
    };
    this.userModal = viewOnly ? 'view' : 'edit';
  }

  private toastOk(key: string): void {
    this.errorMsg = '';
    this.successMsg = this.i18n.instant(key);
    setTimeout(() => {
      this.successMsg = '';
    }, 4000);
  }
}
