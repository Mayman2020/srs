import { test, expect } from '@playwright/test';
import { expectRouteLoads, loginAs } from '../helpers/auth';

test.describe('UAT — dept manager (DEPT_MANAGER)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'deptmgr');
  });

  test('deptmgr dashboard loads with tasks context', async ({ page }) => {
    await expect(page.locator('body')).toContainText(/dashboard|لوحة|task|مهام|department|إدارة/i, {
      timeout: 15_000
    });
  });

  test('deptmgr can open workflow inbox', async ({ page }) => {
    await expectRouteLoads(page, '/workflow-tasks', /workflow|task|مهام|سير/i);
  });

  test('deptmgr can open correspondence list', async ({ page }) => {
    await expectRouteLoads(page, '/correspondence', /correspondence|معامل/i);
  });

  test('deptmgr can open reports', async ({ page }) => {
    await expectRouteLoads(page, '/reports', /report|تقرير|statistic|إحصاء/i);
  });
});
