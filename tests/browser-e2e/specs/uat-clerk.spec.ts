import { test, expect } from '@playwright/test';
import { expectRouteLoads, loginAs } from '../helpers/auth';

test.describe('UAT — clerk (CORRESP_CLERK)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'clerk');
  });

  test('clerk dashboard shows registration focus', async ({ page }) => {
    await expect(page.locator('body')).toContainText(/registration|تسجيل|مكتب/i, { timeout: 15_000 });
  });

  test('clerk can open registration desk', async ({ page }) => {
    await expectRouteLoads(page, '/registration-desk', /registration|تسجيل|مكتب|barcode|باركود/i);
  });

  test('clerk can open outbound delivery', async ({ page }) => {
    await expectRouteLoads(page, '/outbound-delivery', /outbound|صادر|delivery|تسليم/i);
  });

  test('clerk can browse correspondence', async ({ page }) => {
    await expectRouteLoads(page, '/correspondence', /correspondence|معامل/i);
  });
});
