import { defineConfig } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:1200';

export default defineConfig({
  testDir: './specs',
  timeout: 60_000,
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  webServer: process.env.E2E_SKIP_WEB_SERVER
    ? undefined
    : {
        command: 'npm start',
        cwd: '../../SRS_System_frontend',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
