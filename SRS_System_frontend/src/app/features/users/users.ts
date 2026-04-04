import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserDirectoryApiService } from '../../core/api/user-directory-api.service';
import { UserListDto } from '../../core/api/api-types';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

/** View row for the users table; sourced from `GET /api/v1/users` only. */
export interface UserRow {
  id: string;
  name: string;
  username: string;
  departmentCode: string;
  email: string;
  active: boolean;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class UsersComponent implements OnInit {
  users: UserRow[] = [];
  filteredUsers: UserRow[] = [];
  searchQuery = '';
  filterStatus: 'all' | 'active' | 'suspended' = 'all';
  showAddModal = false;
  showEditModal = false;
  selectedUser: UserRow | null = null;

  newUser: Partial<UserRow> = {
    name: '',
    username: '',
    departmentCode: '',
    email: '',
    active: true,
  };

  constructor(
    private userApi: UserDirectoryApiService,
    private i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userApi.list(0, 200).subscribe({
      next: (page) => {
        this.users = (page.content ?? []).map((u) => this.mapDto(u));
        this.applyFilters();
      },
      error: () => {
        this.users = [];
        this.applyFilters();
      },
    });
  }

  private mapDto(u: UserListDto): UserRow {
    return {
      id: u.id,
      name: u.fullNameAr?.trim() || u.fullNameEn?.trim() || u.username,
      username: u.username,
      departmentCode: u.departmentCode ?? '—',
      email: u.email ?? '—',
      active: u.active,
    };
  }

  applyFilters(): void {
    let results = [...this.users];

    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      results = results.filter(
        (u) =>
          u.name.toLowerCase().includes(query) ||
          u.username.toLowerCase().includes(query) ||
          u.email.toLowerCase().includes(query) ||
          u.departmentCode.toLowerCase().includes(query)
      );
    }

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

  openAddModal(): void {
    alert(this.i18n.instant('users.msg.mutationsViaApi'));
  }

  closeAddModal(): void {
    this.showAddModal = false;
  }

  openEditModal(_user: UserRow): void {
    alert(this.i18n.instant('users.msg.mutationsViaApi'));
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUser = null;
  }

  addUser(): void {
    this.openAddModal();
  }

  updateUser(): void {
    this.openEditModal(this.selectedUser!);
  }

  deleteUser(_user: UserRow): void {
    alert(this.i18n.instant('users.msg.mutationsViaApi'));
  }

  toggleUserStatus(_user: UserRow): void {
    alert(this.i18n.instant('users.msg.mutationsViaApi'));
    this.applyFilters();
  }
}
