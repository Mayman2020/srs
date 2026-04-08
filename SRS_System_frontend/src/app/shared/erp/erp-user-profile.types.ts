/** View model derived from {@link AuthSessionSnapshot} for shell + profile UI (immutable per emission). */
export interface ErpUserProfileViewModel {
  rev: number;
  userId: string | null;
  displayName: string;
  initials: string;
  /** Resolved URL for user photo, or null (show initials / no primary img). */
  avatarPrimarySrc: string | null;
  currentRole: string | null;
  roles: readonly string[];
}
