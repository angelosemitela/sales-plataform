import { defineConfig, devices } from '@playwright/test';

/**
 * Testes E2E de interface. A API é SIMULADA com page.route() (ver e2e/mocks.ts):
 * os testes rodam só com o front, sem backend nem banco - rápidos e estáveis.
 * Para um E2E "de verdade" (front + sales-service + Postgres), basta remover os
 * mocks e subir o docker-compose antes.
 */
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
