import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { firstValueFrom, forkJoin } from 'rxjs';
import { CurrentUserProfileApiService } from '../../core/api/current-user-profile-api.service';
import { CurrentUserProfileDto, LookupItemDto } from '../../core/api/api-types';
import { RoleApiService } from '../../core/api/role-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { SrsDatePipe } from '../../shared/pipes/srs-date.pipe';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';
import { ErpUserAvatarComponent } from '../../shared/erp/erp-user-avatar.component';
import { ErpUserProfileStore } from '../../shared/erp/erp-user-profile.store';
import { AuthTokenService } from '../../core/auth/auth-token.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslatePipe,
    LatinDigitsPipe,
    SrsDatePipe,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    ErpUserAvatarComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private static readonly MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024;

  private readonly profileApi = inject(CurrentUserProfileApiService);
  private readonly roleApi = inject(RoleApiService);
  private readonly profileStore = inject(ErpUserProfileStore);
  private readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly notification = inject(NotificationService);
  private readonly tokens = inject(AuthTokenService);

  readonly profile = toSignal(this.profileStore.profile$, {
    initialValue: this.profileStore.snapshot()
  });

  readonly infoForm = this.fb.group({
    fullNameAr: ['', [Validators.required, Validators.maxLength(200)]],
    fullNameEn: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    phone: ['', [Validators.maxLength(32)]],
    nationalId: ['', [Validators.maxLength(32)]]
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmPassword: ['', [Validators.required]]
  });

  detail: CurrentUserProfileDto | null = null;
  roleDirectory: LookupItemDto[] = [];
  loadError = '';
  loading = true;
  savingProfile = false;
  savingPassword = false;
  selectedAvatarFile: File | null = null;
  uploadPreviewUrl: string | null = null;
  selectedTabIndex = 0;

  ngOnInit(): void {
    this.route.fragment.subscribe((fragment) => {
      if (fragment === 'settings') {
        this.selectedTabIndex = 3;
      } else if (fragment === 'password') {
        this.selectedTabIndex = 2;
      }
    });
    this.loadProfilePage();
  }

  displayName(detail: CurrentUserProfileDto | null): string {
    if (!detail) {
      return this.profile().displayName;
    }
    return this.i18n.currentLang() === 'ar' ? detail.fullNameAr : detail.fullNameEn;
  }

  roleLabelById(id: number): string {
    const row = this.roleDirectory.find((role) => role.id === id);
    if (!row) {
      return String(id);
    }
    return this.i18n.currentLang() === 'ar' ? row.nameAr : row.nameEn;
  }

  sessionRoleLabel(code: string): string {
    const key = `roles.codes.${code}`;
    const translated = this.i18n.instant(key);
    return translated === key ? code : translated;
  }

  statusLabel(active: boolean): string {
    return this.i18n.instant(active ? 'users.statusActive' : 'users.statusSuspended');
  }

  departmentLabel(detail: CurrentUserProfileDto | null): string {
    if (!detail) {
      return this.i18n.instant('common.dash');
    }
    const name = this.i18n.currentLang() === 'ar' ? detail.departmentNameAr : detail.departmentNameEn;
    return name?.trim() || detail.departmentCode || this.i18n.instant('common.dash');
  }

  mfaLabel(enabled: boolean | null | undefined): string {
    return this.i18n.instant(enabled ? 'profile.mfaEnabled' : 'profile.mfaDisabled');
  }

  get profileImageSrc(): string | null {
    return this.uploadPreviewUrl || this.profile().avatarPrimarySrc || this.detail?.profileImageUrl || null;
  }

  async saveProfile(): Promise<void> {
    this.infoForm.markAllAsTouched();
    if (this.infoForm.invalid) {
      this.showError('profile.validation.required');
      return;
    }

    this.savingProfile = true;
    const raw = this.infoForm.getRawValue();
    try {
      let detail = await firstValueFrom(
        this.profileApi.updateMyProfile({
        fullNameAr: raw.fullNameAr?.trim() ?? '',
        fullNameEn: raw.fullNameEn?.trim() ?? '',
        email: raw.email?.trim() ?? '',
        phone: raw.phone?.trim() || null,
        nationalId: raw.nationalId?.trim() || null
        })
      );

      if (this.selectedAvatarFile) {
        detail = await firstValueFrom(this.profileApi.uploadMyAvatar(this.selectedAvatarFile));
      }

      this.detail = detail;
      this.patchInfoForm(detail);
      this.resetAvatarSelection();
    } catch (err) {
      void err;
    } finally {
      this.savingProfile = false;
    }
  }

  changePassword(): void {
    this.passwordForm.markAllAsTouched();
    if (this.passwordForm.invalid) {
      this.showError('profile.validation.passwordFormInvalid');
      return;
    }

    const raw = this.passwordForm.getRawValue();
    if ((raw.newPassword ?? '') !== (raw.confirmPassword ?? '')) {
      this.showError('profile.validation.passwordMismatch');
      return;
    }

    this.savingPassword = true;
    this.profileApi
      .updateMyPassword({
        currentPassword: raw.currentPassword ?? '',
        newPassword: raw.newPassword ?? ''
      })
      .subscribe({
        next: () => {
          this.savingPassword = false;
          this.tokens.setMustChangePassword(false);
          this.passwordForm.reset();
          this.loadProfilePage(false);
        },
        error: (err: HttpErrorResponse & { userMessage?: string }) => {
          this.savingPassword = false;
          void err;
        }
      });
  }

  private loadProfilePage(showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
    }
    this.loadError = '';
    forkJoin({
      detail: this.profileApi.getMyProfile(),
      roles: this.roleApi.list()
    }).subscribe({
      next: ({ detail, roles }) => {
        this.detail = detail;
        this.roleDirectory = roles ?? [];
        this.patchInfoForm(detail);
        this.loading = false;
      },
      error: (err: HttpErrorResponse & { userMessage?: string }) => {
        this.loading = false;
        this.loadError = this.resolveErrorMessage(err, 'profile.loadError');
      }
    });
  }

  private patchInfoForm(detail: CurrentUserProfileDto): void {
    this.infoForm.reset({
      fullNameAr: detail.fullNameAr ?? '',
      fullNameEn: detail.fullNameEn ?? '',
      email: detail.email ?? '',
      phone: detail.phone ?? '',
      nationalId: detail.nationalId ?? ''
    });
  }

  private resolveErrorMessage(
    err: (HttpErrorResponse & { userMessage?: string }) | null | undefined,
    fallbackKey: string
  ): string {
    const raw = err?.userMessage?.trim() || err?.message?.trim() || '';
    if (raw) {
      const translated = this.i18n.instant(raw);
      return translated === raw ? raw : translated;
    }
    return this.i18n.instant(fallbackKey);
  }

  private showError(key: string): void {
    this.notification.warning(key);
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedAvatarFile = file;
    if (!file) {
      this.uploadPreviewUrl = null;
      return;
    }

    if (file.size > ProfileComponent.MAX_AVATAR_FILE_SIZE) {
      this.resetAvatarSelection();
      input.value = '';
      this.notification.warning('profile.validation.avatarTooLarge');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.uploadPreviewUrl = typeof reader.result === 'string' ? reader.result : null;
    };
    reader.readAsDataURL(file);
  }

  private resetAvatarSelection(): void {
    this.selectedAvatarFile = null;
    this.uploadPreviewUrl = null;
  }
}
