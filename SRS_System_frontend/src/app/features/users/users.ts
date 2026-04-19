import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
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
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';
import { NotificationService } from '../../core/services/notification.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';
import { matchesTableSearch } from '../../core/util/table-text-filter';
import {
  MultiChoiceOption,
  MultiChoiceTableComponent
} from '../../shared/components/multi-choice-table/multi-choice-table.component';

/** Standalone users page (original app layout) backed by `/api/v1/users`. */
@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    ErpAutoReferenceFieldComponent,
    SrsDataTableComponent,
    MultiChoiceTableComponent
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class UsersComponent implements OnInit {
  users: UserListDto[] = [];
  filteredUsers: UserListDto[] = [];
  departments: DepartmentFlatDto[] = [];
  rolesLookup: LookupItemDto[] = [];

  searchQuery = '';
  filterStatus: 'all' | 'active' | 'suspended' = 'all';

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
  } = this.emptyUserForm();

  assignRoleIds: number[] = [];
  assignTargetUserId: string | null = null;

  constructor(
    private userApi: UserDirectoryApiService,
    private deptApi: DepartmentApiService,
    private roleApi: RoleApiService,
    private i18n: I18nService,
    private notification: NotificationService,
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
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (value) => (this.loading = value),
      source: this.userApi.list(0, 500),
      next: (page) => {
        this.errorMsg = '';
        this.users = page.content ?? [];
        this.applyFilters();
      },
      error: () => {
        this.users = [];
        this.filteredUsers = [];
      }
    });
  }

  private syncViewAfterAsyncMutation(work: () => void): void {
    work();
    queueMicrotask(() => {
      try {
        this.cdr.detectChanges();
      } catch {
        /* ignore detached views */
      }
    });
  }

  displayName(u: UserListDto): string {
    return u.fullNameAr?.trim() || u.fullNameEn?.trim() || u.username;
  }

  applyFilters(): void {
    let results = [...this.users];
    results = results.filter((u) =>
      matchesTableSearch(this.searchQuery, [
        this.displayName(u),
        u.fullNameAr,
        u.fullNameEn,
        u.username,
        u.email,
        u.departmentCode,
        u.active,
        this.statusLabel(u.active)
      ])
    );
    if (this.filterStatus === 'active') {
      results = results.filter((u) => u.active);
    } else if (this.filterStatus === 'suspended') {
      results = results.filter((u) => !u.active);
    }
    this.filteredUsers = results;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  statusLabel(active: boolean): string {
    return this.i18n.instant(active ? 'users.statusActive' : 'users.statusSuspended');
  }

  labelRoleCard(r: LookupItemDto): string {
    return r.nameAr?.trim() || r.nameEn?.trim() || r.code;
  }

  openAddModal(): void {
    this.userForm = this.emptyUserForm();
    this.userModal = 'add';
  }

  openViewModal(u: UserListDto): void {
    this.userApi.getOne(u.id).subscribe({
      next: (d) => this.syncViewAfterAsyncMutation(() => this.patchUserFormFromDetail(d, true)),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openEditModal(u: UserListDto): void {
    this.userApi.getOne(u.id).subscribe({
      next: (d) => this.syncViewAfterAsyncMutation(() => this.patchUserFormFromDetail(d, false)),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openAssignModal(u: UserListDto): void {
    this.syncViewAfterAsyncMutation(() => {
      this.assignTargetUserId = u.id;
      this.assignRoleIds = [];
      this.userModal = 'assign';
    });
    this.userApi.getOne(u.id).subscribe({
      next: (d) =>
        this.syncViewAfterAsyncMutation(() => {
          this.assignRoleIds = this.normalizeRoleIds(d.roleIds);
        }),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  closeUserModal(): void {
    this.syncViewAfterAsyncMutation(() => {
      this.userModal = null;
      this.assignTargetUserId = null;
      this.assignRoleIds = [];
    });
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
    if (!this.assignTargetUserId || this.assignRoleIds.length === 0) {
      return;
    }
    this.userApi.assignRoles(this.assignTargetUserId, this.assignRoleIds).subscribe({
      next: () => {
        this.userModal = null;
        this.assignTargetUserId = null;
        this.assignRoleIds = [];
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

  get roleMultiOptions(): MultiChoiceOption[] {
    return this.rolesLookup.map((role) => ({
      id: role.id,
      label: this.labelRoleCard(role),
      code: role.code,
      subtitle: role.nameEn?.trim() && role.nameEn !== this.labelRoleCard(role) ? role.nameEn : null
    }));
  }

  onAssignRoleIdsChange(ids: readonly (number | string)[]): void {
    this.assignRoleIds = this.normalizeRoleIds(ids.map((id) => Number(id)));
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

  private normalizeRoleIds(roleIds: readonly number[] | null | undefined): number[] {
    if (!roleIds?.length) {
      return [];
    }
    const unique = new Set<number>();
    for (const roleId of roleIds) {
      if (Number.isFinite(roleId)) {
        unique.add(roleId);
      }
    }
    return [...unique];
  }

  private toastOk(key: string): void {
    this.errorMsg = '';
    this.successMsg = '';
    this.notification.success(key);
  }
}
