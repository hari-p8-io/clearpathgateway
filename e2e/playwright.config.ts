import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  use: {
    trace: 'on-first-retry'
  },
  projects: [
    { name: 'Chromium', use: { ...devices['Desktop Chrome'] } }
  ]
});


