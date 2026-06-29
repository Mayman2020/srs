import { test } from '@playwright/test';
import { expectRouteLoads, loginAs } from '../helpers/auth';

test.describe('Business gap screens — smoke', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'clerk');
  });

  test('registration desk loads', async ({ page }) => {
    await expectRouteLoads(page, '/registration-desk', /registration|تسجيل|مكتب/i);
  });

  test('outbound delivery loads', async ({ page }) => {
    await expectRouteLoads(page, '/outbound-delivery', /outbound|صادر|delivery|تسليم/i);
  });

  test('circular read report loads', async ({ page }) => {
    await expectRouteLoads(page, '/circulars/read-report', /circular|تعميم|read|قراءة/i);
  });

  test('workflow tasks inbox loads', async ({ page }) => {
    await expectRouteLoads(page, '/workflow-tasks', /workflow|task|مهام|سير/i);
  });

  test('correspondence list loads', async ({ page }) => {
    await expectRouteLoads(page, '/correspondence', /correspondence|معامل/i);
  });
});
