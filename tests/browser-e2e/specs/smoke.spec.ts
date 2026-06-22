import { test, expect } from '@playwright/test';

const USER = process.env.E2E_USERNAME || 'admin';
const PASS = process.env.E2E_PASSWORD || 'admin';

test.describe('Administrative Communications — critical paths', () => {
  test('login reaches dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[formcontrolname="username"]').fill(USER);
    await page.locator('input[formcontrolname="password"]').fill(PASS);
    await page.getByRole('button', { name: /login|sign in|دخول/i }).click();
    await expect(page).toHaveURL(/dashboard/, { timeout: 30_000 });
  });

  test('correspondence list loads for authenticated user', async ({ page }) => {
    await page.goto('/login');
    await page.locator('input[formcontrolname="username"]').fill(USER);
    await page.locator('input[formcontrolname="password"]').fill(PASS);
    await page.getByRole('button', { name: /login|sign in|دخول/i }).click();
    await page.waitForURL(/dashboard/, { timeout: 30_000 });
    await page.goto('/correspondence');
    await expect(page.locator('body')).toContainText(/correspondence|معامل/i, {
      timeout: 20_000,
    });
  });
});
