/** Immutable snapshot of auth/session fields read by shell UI (topbar, etc.). */
export interface AuthSessionSnapshot {
  /** Increments on each session mutation so consumers can bust caches (e.g. avatar URL). */
  rev: number;
  username: string | null;
  userId: string | null;
  profileImageUrl: string | null;
  roles: string[];
  currentRole: string | null;
}
