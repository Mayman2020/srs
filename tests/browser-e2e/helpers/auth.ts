import { Page, expect } from '@playwright/test';

export type DemoRole = 'clerk' | 'deptmgr' | 'auditor' | 'admin' | 'manager';

const ROLE_ENV: Record<DemoRole, { user: string; pass: string }> = {
  clerk: { user: 'clerk', pass: 'clerk' },
  deptmgr: { user: 'deptmgr', pass: 'deptmgr' },
  auditor: { user: 'auditor', pass: 'auditor' },
  admin: { user: 'admin', pass: 'admin' },
  manager: { user: 'manager', pass: 'manager' }
};

export function credentialsFor(role: DemoRole): { user: string; pass: string } {
  const fromEnv =
    role === 'clerk'
      ? { user: process.env.E2E_CLERK_USER, pass: process.env.E2E_CLERK_PASS }
      : role === 'deptmgr'
        ? { user: process.env.E2E_DEPTMGR_USER, pass: process.env.E2E_DEPTMGR_PASS }
        : role === 'auditor'
          ? { user: process.env.E2E_AUDITOR_USER, pass: process.env.E2E_AUDITOR_PASS }
          : { user: process.env.E2E_USERNAME, pass: process.env.E2E_PASSWORD };

  const fallback = ROLE_ENV[role];
  return {
    user: fromEnv.user || fallback.user,
    pass: fromEnv.pass || fallback.pass
  };
}

export async function loginAs(page: Page, role: DemoRole): Promise<void> {
  const { user, pass } = credentialsFor(role);
  await page.goto('/login');
  await page.locator('input[formcontrolname="username"]').fill(user);
  await page.locator('input[formcontrolname="password"]').fill(pass);
  await page.getByRole('button', { name: /login|sign in|دخول/i }).click();
  await expect(page).toHaveURL(/dashboard/, { timeout: 45_000 });
}

export async function expectRouteLoads(page: Page, path: string, bodyPattern: RegExp): Promise<void> {
  await page.goto(path);
  await expect(page.locator('body')).toContainText(bodyPattern, { timeout: 25_000 });
}
