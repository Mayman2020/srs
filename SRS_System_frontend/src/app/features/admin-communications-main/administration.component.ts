import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { I18nService } from '../../core/i18n/i18n.service';
import { EffectivePermissionDto, UserDirectoryApiService } from '../../core/api/user-directory-api.service';
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
import { NotificationService } from '../../core/services/notification.service';
import { DialogService } from '../../core/services/dialog.service';
import { ErpAutoReferenceFieldComponent } from '../../shared/erp/erp-auto-reference-field.component';
import { CapabilitiesService } from '../../core/auth/capabilities.service';
import { matchesTableSearch } from '../../core/util/table-text-filter';
import {
  MultiChoiceOption,
  MultiChoiceTableComponent
} from '../../shared/components/multi-choice-table/multi-choice-table.component';
import { SrsDataTableComponent } from '../../shared/data-table/srs-data-table.component';

export type AdminTab = 'users' | 'permissions' | 'screens' | 'roles' | 'issues';

@Component({
  selector: 'app-administration',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    LatinDigitsPipe,
    SrsDatePipe,
    ErpAutoReferenceFieldComponent,
    MultiChoiceTableComponent,
    SrsDataTableComponent
  ],
  templateUrl: './administration.component.html',
  styleUrl: './administration.component.scss'
})
export class AdministrationComponent implements OnInit {
  activeTab: AdminTab = 'users';

  users: UserListDto[] = [];
  filteredUsers: UserListDto[] = [];
  usersPageIndex = 0;
  usersPageSize = 20;
  usersTotal = 0;
  readonly togglingUserIds = new Set<string>();
  searchQuery = '';
  filterStatus: 'all' | 'active' | 'suspended' = 'all';
  departments: DepartmentFlatDto[] = [];
  rolesLookup: LookupItemDto[] = [];

  permissions: PermissionAdminDto[] = [];
  screens: UiScreenDto[] = [];
  issues: SystemIssueDto[] = [];
  permissionsSearchQuery = '';
  screensSearchQuery = '';
  issuesSearchQuery = '';

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
  assignRoleIds: number[] = [];
  assignTargetUserId: string | null = null;
  effectivePermissions: EffectivePermissionDto[] = [];

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
    private notification: NotificationService,
    private readonly dialogService: DialogService,
    private readonly cdr: ChangeDetectorRef,
    private readonly cap: CapabilitiesService
  ) {}

  ngOnInit(): void {
    const def = this.route.snapshot.data['defaultAdminTab'] as AdminTab | undefined;
    if (def && this.isTab(def)) {
      this.activeTab = def;
    }
    if (!this.canOpenTab(this.activeTab)) {
      this.activeTab = (['users', 'permissions', 'screens', 'roles', 'issues'] as AdminTab[])
        .find((tab) => this.canOpenTab(tab)) ?? 'users';
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
      source: this.usersApi.listRoles(),
      next: (r) => (this.rolesLookup = r ?? []),
      error: () => (this.rolesLookup = [])
    });
  }

  private isTab(t: string): t is AdminTab {
    return ['users', 'permissions', 'screens', 'roles', 'issues'].includes(t);
  }

  canOpenTab(tab: AdminTab): boolean {
    const permission: Record<AdminTab, string> = {
      users: 'ADMIN_USER_MANAGE',
      permissions: 'ADMIN_ROLE_MANAGE',
      screens: 'ADMIN_UI_SCREEN_MANAGE',
      roles: 'ADMIN_ROLE_MANAGE',
      issues: 'ADMIN_AUDIT_VIEW'
    };
    return this.cap.can(permission[tab]);
  }

  selectTab(tab: AdminTab): void {
    if (!this.canOpenTab(tab)) return;
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
        source: forkJoin({
          permissions: this.adminApi.listPermissions(),
          roles: this.roleApi.list()
        }),
        next: ({ permissions, roles }) => {
          this.permissions = permissions ?? [];
          this.rolesLookup = roles ?? [];
          if (this.rolesLookup.length) {
            const has =
              this.matrixRoleId != null &&
              this.rolesLookup.some((r) => r.id === this.matrixRoleId);
            if (this.matrixRoleId == null || !has) {
              this.matrixRoleId = this.rolesLookup[0].id;
            }
          } else {
            this.matrixRoleId = null;
          }
          this.loadMatrix();
        },
        error: (err) => {
          this.permissions = [];
          this.rolesLookup = [];
          this.matrixChecks = [];
          this.matrixRoleId = null;
          this.setLoadError(err);
        }
      });
    } else if (this.activeTab === 'issues') {
      this.loadIssues();
    }
  }

  loadUsers(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (value) => (this.loading = value),
      source: this.usersApi.list(this.usersPageIndex, this.usersPageSize, this.searchQuery),
      next: (p) => {
        this.errorMsg = '';
        this.users = p.content ?? [];
        this.usersTotal = p.totalElements ?? 0;
        this.applyFilters();
      },
      error: (err) => {
        this.users = [];
        this.filteredUsers = [];
        this.setLoadError(err);
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
    this.usersPageIndex = 0;
    this.loadUsers();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  changeUsersPage(delta: number): void {
    const next = this.usersPageIndex + delta;
    if (next < 0 || next * this.usersPageSize >= this.usersTotal) return;
    this.usersPageIndex = next;
    this.loadUsers();
  }

  statusLabel(active: boolean): string {
    return this.i18n.instant(active ? 'users.statusActive' : 'users.statusSuspended');
  }

  loadPermissions(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: this.adminApi.listPermissions(),
      next: (perms) => {
        this.permissions = perms ?? [];
      },
      error: (err) => {
        this.permissions = [];
        this.screens = [];
        this.setLoadError(err);
      }
    });
  }

  get filteredPermissions(): PermissionAdminDto[] {
    return this.permissions.filter((permission) =>
      matchesTableSearch(this.permissionsSearchQuery, [
        permission.code,
        permission.nameAr,
        permission.nameEn,
        permission.description,
        permission.sortOrder,
        permission.active,
        this.labelScreen(permission.uiScreenId)
      ])
    );
  }

  loadScreens(): void {
    subscribePageLoad({
      cdr: this.cdr,
      source: forkJoin({
        screens: this.adminApi.listUiScreens(),
        permissions: this.adminApi.listPermissions()
      }),
      next: ({ screens, permissions }) => {
        this.screens = screens ?? [];
        this.permissions = permissions ?? [];
      },
      error: (err) => {
        this.screens = [];
        this.permissions = [];
        this.setLoadError(err);
      }
    });
  }

  get filteredScreens(): UiScreenDto[] {
    return this.screens.filter((screen) =>
      matchesTableSearch(this.screensSearchQuery, [
        screen.code,
        screen.routePath,
        screen.iconKey,
        screen.nameAr,
        screen.nameEn,
        screen.description,
        screen.active,
        screen.showInShellNav,
        this.screenRequiredPermLabel(screen),
        this.i18n.instant(screen.showInShellNav ? 'admin.shellNavYes' : 'admin.shellNavNo')
      ])
    );
  }

  loadIssues(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (value) => (this.loading = value),
      source: this.adminApi.listSystemIssues(),
      next: (r) => {
        this.issues = r ?? [];
      },
      error: (err) => {
        this.issues = [];
        this.setLoadError(err);
      }
    });
  }

  get filteredIssues(): SystemIssueDto[] {
    return this.issues.filter((issue) =>
      matchesTableSearch(this.issuesSearchQuery, [
        issue.source,
        issue.severity,
        issue.message,
        issue.detail,
        issue.pageUrl,
        issue.userId,
        issue.httpStatus,
        issue.createdAt,
        issue.resolvedAt,
        issue.resolutionNote,
        this.issueResolved(issue),
        this.i18n.instant(this.issueResolved(issue) ? 'common.toastOk' : 'common.empty')
      ])
    );
  }

  loadMatrix(): void {
    if (this.matrixRoleId == null) {
      this.matrixChecks = [];
      return;
    }
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (value) => (this.matrixSaving = value),
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
      error: (err) => {
        this.matrixChecks = [];
        this.setLoadError(err);
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
      setLoading: (value) => (this.matrixSaving = value),
      source: this.adminApi.saveRolePermissionIds(this.matrixRoleId, ids),
      next: () => {
        this.toastOk('admin.matrixSaved');
        // Reload capabilities so the UI reflects new permissions immediately (acts like re-login)
        this.cap.clear();
        this.cap.load().subscribe({
          complete: () => window.location.reload(),
          error: () => window.location.reload()
        });
      },
      error: (e: unknown) => {
        const err = e as HttpErrorResponse & { userMessage?: string };
        this.errorMsg = err.userMessage ?? this.i18n.instant('admin.saveFailed');
        this.cdr.detectChanges();
      }
    });
  }

  openAddUser(): void {
    this.userForm = this.emptyUserForm();
    this.userModal = 'add';
  }

  openViewUser(u: UserListDto): void {
    this.usersApi.getOne(u.id).subscribe({
      next: (d) => this.syncViewAfterAsyncMutation(() => this.patchUserFormFromDetail(d, true)),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openEditUser(u: UserListDto): void {
    this.usersApi.getOne(u.id).subscribe({
      next: (d) => this.syncViewAfterAsyncMutation(() => this.patchUserFormFromDetail(d, false)),
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
  }

  openAssignRole(u: UserListDto): void {
    this.syncViewAfterAsyncMutation(() => {
      this.assignTargetUserId = u.id;
      this.assignRoleIds = [];
      this.effectivePermissions = [];
      this.userModal = 'assign';
    });
    forkJoin({
      detail: this.usersApi.getOne(u.id),
      effective: this.usersApi.effectivePermissions(u.id)
    }).subscribe({
      next: ({ detail, effective }) => {
        this.syncViewAfterAsyncMutation(() => {
          this.assignRoleIds = this.normalizeRoleIds(detail.roleIds);
          this.effectivePermissions = effective ?? [];
        });
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.loadUserFailed');
      }
    });
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

  deleteUser(u: UserListDto): void {
    this.dialogService.openConfirm({
      titleKey: 'admin.confirmDelete',
      messageKey: 'admin.confirmDeleteUser',
      confirmButton: { labelKey: 'common.delete', color: 'warn' },
      cancelButton: { labelKey: 'common.close' }
    }).subscribe((ok) => {
      if (!ok) return;
      this.usersApi.delete(u.id).subscribe({
        next: () => {
          this.loadUsers();
          this.toastOk('admin.userDeleted');
        },
        error: (e: HttpErrorResponse & { userMessage?: string }) => {
          this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
        }
      });
    });
  }

  toggleUserStatus(u: UserListDto): void {
    if (this.togglingUserIds.has(u.id)) return;
    this.togglingUserIds.add(u.id);
    this.usersApi.toggleActive(u.id).subscribe({
      next: () => {
        this.togglingUserIds.delete(u.id);
        this.loadUsers();
        this.toastOk('admin.userUpdated');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.togglingUserIds.delete(u.id);
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  saveAssignRole(): void {
    if (!this.assignTargetUserId || this.assignRoleIds.length === 0) {
      return;
    }
    this.usersApi.assignRoles(this.assignTargetUserId, this.assignRoleIds).subscribe({
      next: () => {
        this.userModal = null;
        this.assignTargetUserId = null;
        this.assignRoleIds = [];
        this.effectivePermissions = [];
        this.loadUsers();
        this.toastOk('admin.roleAssigned');
      },
      error: (e: HttpErrorResponse & { userMessage?: string }) => {
        this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
      }
    });
  }

  closeUserModal(): void {
    this.syncViewAfterAsyncMutation(() => {
      this.userModal = null;
      this.assignTargetUserId = null;
      this.assignRoleIds = [];
      this.effectivePermissions = [];
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
      active: p.active,
      uiScreenId: p.uiScreenId ?? null
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
      active: !!this.permForm.active,
      uiScreenId: this.permForm.uiScreenId ?? null
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
    this.dialogService.openConfirm({
      titleKey: 'admin.confirmDelete',
      messageKey: 'admin.confirmDelete',
      confirmButton: { labelKey: 'common.delete', color: 'warn' },
      cancelButton: { labelKey: 'common.close' }
    }).subscribe((ok) => {
      if (!ok) return;
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
      active: s.active,
      iconKey: (s.iconKey ?? 'apps').trim() || 'apps',
      showInShellNav: !!s.showInShellNav,
      requiredPermissionId: s.requiredPermissionId ?? null
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
      active: !!this.screenForm.active,
      iconKey: (this.screenForm.iconKey ?? 'apps').trim() || 'apps',
      showInShellNav: !!this.screenForm.showInShellNav,
      requiredPermissionId: this.screenForm.requiredPermissionId ?? null
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
    this.dialogService.openConfirm({
      titleKey: 'admin.confirmDelete',
      messageKey: 'admin.confirmDelete',
      confirmButton: { labelKey: 'common.delete', color: 'warn' },
      cancelButton: { labelKey: 'common.close' }
    }).subscribe((ok) => {
      if (!ok) return;
      this.adminApi.deleteUiScreen(s.id).subscribe({
        next: () => {
          this.loadScreens();
          this.toastOk('admin.deleted');
        },
        error: (e: HttpErrorResponse & { userMessage?: string }) => {
          this.errorMsg = e.userMessage ?? this.i18n.instant('admin.saveFailed');
        }
      });
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

  get checkedPermCount(): number {
    return this.matrixChecks.filter((c) => c.checked).length;
  }

  labelRoleCard(r: LookupItemDto): string {
    return r.nameAr?.trim() || r.nameEn?.trim() || r.code;
  }

  labelScreen(id: number | null | undefined): string {
    if (id == null) {
      return '—';
    }
    const s = this.screens.find((x) => x.id === id);
    return s ? s.code : `#${id}`;
  }

  labelPermission(p: PermissionAdminDto): string {
    return p.nameAr?.trim() || p.nameEn?.trim() || p.code;
  }

  screenRequiredPermLabel(screen: UiScreenDto): string {
    const id = screen.requiredPermissionId;
    if (id == null) {
      return '';
    }
    const p = this.permissions.find((x) => x.id === id);
    return p ? this.labelPermission(p) : `#${id}`;
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
      active: true,
      uiScreenId: null as number | null
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
      active: true,
      iconKey: 'apps',
      showInShellNav: false,
      requiredPermissionId: null as number | null
    };
  }

  private toastOk(key: string): void {
    this.errorMsg = '';
    this.successMsg = '';
    this.notification.success(key);
  }

  private setLoadError(err: unknown): void {
    const e = err as HttpErrorResponse & { userMessage?: string };
    if (e.userMessage) {
      this.errorMsg = e.userMessage;
      return;
    }
    if (e.status === 403) {
      this.errorMsg = this.i18n.instant('errors.forbidden');
      return;
    }
    if (e.status === 0) {
      this.errorMsg = this.i18n.instant('errors.network');
      return;
    }
    this.errorMsg = this.i18n.instant('errors.generic');
  }
}
