import { Injectable } from '@angular/core';

const STORAGE_KEY = 'ac_access_token';
const USERNAME_KEY = 'ac_username';

@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  getToken(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }

  setToken(token: string): void {
    try {
      localStorage.setItem(STORAGE_KEY, token);
    } catch {
      /* ignore */
    }
  }

  getUsername(): string | null {
    try {
      return localStorage.getItem(USERNAME_KEY);
    } catch {
      return null;
    }
  }

  setUsername(username: string | null): void {
    try {
      if (username) {
        localStorage.setItem(USERNAME_KEY, username);
      } else {
        localStorage.removeItem(USERNAME_KEY);
      }
    } catch {
      /* ignore */
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(USERNAME_KEY);
    } catch {
      /* ignore */
    }
  }
}
