import { test, expect } from '@playwright/test';
import { loginAs } from '../helpers/auth';
import {
  VIEWPORTS,
  closeMobileNavViaBackdrop,
  expectNoHorizontalOverflow,
  expectSidebarPosition,
  openMobileNav
} from '../helpers/responsive';

for (const viewport of VIEWPORTS) {
  test.describe(`Responsive shell — ${viewport.name} (${viewport.width}px)`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test.beforeEach(async ({ page }) => {
      await loginAs(page, 'admin');
      await page.goto('/dashboard');
      await expect(page.locator('.main-layout__content')).toBeVisible({ timeout: 20_000 });
    });

    test('dashboard has no horizontal overflow', async ({ page }) => {
      await expectNoHorizontalOverflow(page);
    });

    test('main content uses full shell width', async ({ page }) => {
      const layout = await page.locator('.main-layout').boundingBox();
      const content = await page.locator('.main-layout__content').boundingBox();
      expect(layout).toBeTruthy();
      expect(content).toBeTruthy();
      if (layout && content) {
        expect(content.width).toBeGreaterThan(layout.width * 0.55);
      }
    });

    if (viewport.isMobile) {
      test('mobile nav drawer opens and closes with backdrop', async ({ page }) => {
        await expect(page.locator('html')).not.toHaveClass(/sidebar-open/);
        await expect(page.locator('.main-layout__backdrop')).toHaveCount(0);
        await expectSidebarPosition(page, 'fixed');

        await openMobileNav(page);
        await closeMobileNavViaBackdrop(page);
        await expectNoHorizontalOverflow(page);
      });

      test('mobile nav closes after choosing a menu item', async ({ page }) => {
        await openMobileNav(page);
        const navItem = page.locator('app-sidebar .nav .item').first();
        await expect(navItem).toBeVisible();
        await navItem.click();
        await expect(page.locator('html')).not.toHaveClass(/sidebar-open/);
      });
    } else {
      test('desktop sidebar stays in document flow', async ({ page }) => {
        await expect(page.locator('html')).not.toHaveClass(/sidebar-open/);
        await expect(page.locator('.main-layout__backdrop')).toHaveCount(0);
        await expectSidebarPosition(page, 'relative');
      });
    }
  });
}

test.describe('Responsive shell — correspondence list', () => {
  test.use({ viewport: { width: 768, height: 1024 } });

  test('correspondence page has no horizontal overflow on tablet', async ({ page }) => {
    await loginAs(page, 'admin');
    await page.goto('/correspondence');
    await expect(page.locator('body')).toContainText(/correspondence|معامل/i, { timeout: 25_000 });
    await expectNoHorizontalOverflow(page);
  });
});
