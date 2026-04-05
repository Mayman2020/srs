import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
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

export type AdminTab = 'users' | 'permissions' | 'screens' | 'roles' | 'issues';

@Component({
  selector: 'app-administration',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './administration.component.html',
  styleUrl: './administration.component.scss'
})
export class AdministrationComponent implements OnInit {
  activeTab: AdminTab = 'users';

  users: UserListDto[] = [];
  departments: DepartmentFlatDto[] = [];
  rolesLookup: LookupItemDto[] = [];

  permissions: PermissionAdminDto[] = [];
  screens: UiScreenDto[] = [];
  issues: SystemIssueDto[] = [];

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
    private i18n: I18nService
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
    this.deptApi.list().subscribe({
      next: (rows) => (this.departments = rows ?? []),
      error: () => (this.departments = [])
    });
    this.roleApi.list().subscribe({
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
      this.adminApi.listPermissions().subscribe({
        next: (p) => {
          this.permissions = p ?? [];
          if (!this.matrixRoleId && this.rolesLookup.length) {
            this.matrixRoleId = this.rolesLookup[0].id;
          }
          this.loadMatrix();
        },
        error: () => {
          this.permissions = [];
          this.matrixChecks = [];
        }
      });
    } else if (this.activeTab === 'issues') {
      this.loadIssues();
    }
  }

  loadUsers(): void {
    this.loading = true;
    this.usersApi.list(0, 500).subscribe({
      next: (p) => {
        this.users = p.content ?? [];
        this.loading = false;
      },
      error: () => {
        this.users = [];
        this.loading = false;
      }
    });
  }

  loadPermissions(): void {
    this.adminApi.listPermissions().subscribe({
      next: (r) => (this.permissions = r ?? []),
      error: () => (this.permissions = [])
    });
  }

  loadScreens(): void {
    this.adminApi.listUiScreens().subscribe({
      next: (r) => (this.screens = r ?? []),
      error: () => (this.screens = [])
    });
  }

  loadIssues(): void {
    this.loading = true;
    this.adminApi.listSystemIssues().subscribe({
      next: (r) => {
        this.issues = r ?? [];
        this.loading = false;
      },
      error: () => {
        this.issues = [];
        this.loading = false;
      }
    });
  }

  loadMatrix(): void {
    if (this.matrixRoleId == null) {
      this.matrixChecks = [];
      return;
    }
    this.matrixSaving = true;
    this.adminApi.getRolePermissionIds(this.matrixRoleId).subscribe({
      next: (ids) => {
        const set = new Set(ids ?? []);
        this.matrixChecks = (this.permissions.length ? this.permissions : []).map((p) => ({
          id: p.id,
          code: p.code,
          label: this.labelPermission(p),
          checked: set.has(p.id)
        }));
        this.matrixSaving = false;
      },
      error: () => {
        this.matrixChecks = [];
        this.matrixSaving = false;
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
    this.matrixSaving = true;
    this.adminApi.saveRolePermissionIds(this.matrixRoleId, ids).subscribe({
      next: () => {
        this.matrixSaving = false;
        this.toastOk('admin.matrixSaved');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.matrixSaving = false;
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
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
    return r.nameAr?.trim() || r.nameEn?.trim() || r.code;
  }

  labelPermission(p: PermissionAdminDto): string {
    return p.nameAr?.trim() || p.nameEn?.trim() || p.code;
  }

  issueResolved(i: SystemIssueDto): boolean {
    return !!i.resolvedAt;
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
