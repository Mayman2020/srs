import { expect, Page } from '@playwright/test';

export type ViewportPreset = {
  name: string;
  width: number;
  height: number;
  isMobile: boolean;
};

export const VIEWPORTS: readonly ViewportPreset[] = [
  { name: 'mobile', width: 375, height: 812, isMobile: true },
  { name: 'tablet', width: 768, height: 1024, isMobile: true },
  { name: 'desktop', width: 1280, height: 800, isMobile: false }
] as const;

/** Fails when the page is wider than the viewport (unintended horizontal scroll). */
export async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const metrics = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }));
  expect(metrics.scrollWidth).toBeLessThanOrEqual(metrics.clientWidth + 2);
}

export async function expectSidebarPosition(
  page: Page,
  mode: 'fixed' | 'relative'
): Promise<void> {
  const position = await page.locator('app-sidebar .sidebar').evaluate((el) => getComputedStyle(el).position);
  expect(position).toBe(mode);
}

export async function openMobileNav(page: Page): Promise<void> {
  const toggle = page.locator('button.menu-toggle').first();
  await toggle.scrollIntoViewIfNeeded();
  await expect(toggle).toBeVisible();
  await toggle.click();
  await expect(page.locator('html')).toHaveClass(/sidebar-open/);
  await expect(page.locator('.main-layout__backdrop')).toBeVisible();
}

export async function closeMobileNavViaBackdrop(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  const x = viewport ? Math.max(16, Math.floor(viewport.width * 0.12)) : 16;
  const y = viewport ? Math.floor(viewport.height * 0.45) : 400;
  await page.mouse.click(x, y);
  await expect(page.locator('html')).not.toHaveClass(/sidebar-open/);
  await expect(page.locator('.main-layout__backdrop')).toHaveCount(0);
}
