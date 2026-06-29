import { test, expect } from '@playwright/test';
import { expectRouteLoads, loginAs } from '../helpers/auth';

test.describe('UAT — auditor (AUDITOR)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'auditor');
  });

  test('auditor dashboard shows compliance focus', async ({ page }) => {
    await expect(page.locator('body')).toContainText(/audit|تدقيق|compliance|امتثال|attachment|مرفق/i, {
      timeout: 15_000
    });
  });

  test('auditor can open audit events', async ({ page }) => {
    await expectRouteLoads(page, '/audit-events', /audit|تدقيق|event|حدث/i);
  });

  test('auditor can open attachment access log', async ({ page }) => {
    await expectRouteLoads(page, '/admin/attachment-access-log', /attachment|مرفق|access|وصول|log|سجل/i);
  });

  test('auditor can open reports (read-only)', async ({ page }) => {
    await expectRouteLoads(page, '/reports', /report|تقرير|statistic|إحصاء/i);
  });

  test('auditor can browse correspondence read-only', async ({ page }) => {
    await expectRouteLoads(page, '/correspondence', /correspondence|معامل/i);
  });
});
