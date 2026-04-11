import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RoleApiService } from '../../core/api/role-api.service';
import { AdminConsoleApiService } from '../../core/api/admin-console-api.service';
import { LookupItemDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { subscribePageLoad } from '../../core/rxjs/subscribe-page-load';

export interface RoleCardVm {
  id: string;
  code: string;
  name: string;
  description: string;
  usersCount: number;
  permissions: { name: string; enabled: boolean }[];
}

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, TranslatePipe, RouterLink],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class RolesComponent implements OnInit {
  roles: RoleCardVm[] = [];
  selectedRole: RoleCardVm | null = null;
  showPermissionsModal = false;
  permsLoadError = false;
  loading = false;

  constructor(
    private roleApi: RoleApiService,
    private adminApi: AdminConsoleApiService,
    private i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    subscribePageLoad({
      cdr: this.cdr,
      setLoading: (value) => (this.loading = value),
      source: this.roleApi.list(),
      next: (items) => {
        this.roles = (items ?? []).map((r) => this.mapRole(r));
      },
      error: () => {
        this.roles = [];
      },
    });
  }

  private mapRole(r: LookupItemDto): RoleCardVm {
    return {
      id: String(r.id),
      code: r.code,
      name: r.nameAr?.trim() || r.nameEn?.trim() || r.code,
      description: r.nameEn?.trim() ? r.nameEn : r.code,
      usersCount: 0,
      permissions: [],
    };
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
}
