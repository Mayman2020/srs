import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RoleApiService } from '../../core/api/role-api.service';
import { LookupItemDto } from '../../core/api/api-types';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

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
  imports: [CommonModule, TranslatePipe],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class RolesComponent implements OnInit {
  roles: RoleCardVm[] = [];
  selectedRole: RoleCardVm | null = null;
  showPermissionsModal = false;

  constructor(private roleApi: RoleApiService) {}

  ngOnInit(): void {
    this.roleApi.list().subscribe({
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
    this.selectedRole = role;
    this.showPermissionsModal = true;
  }

  closeModal(): void {
    this.showPermissionsModal = false;
    this.selectedRole = null;
  }
}
