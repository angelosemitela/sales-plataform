import type { Page } from '@playwright/test';

/** API simulada: os testes de interface não dependem de backend nem de banco. */
export const countries = [
  { code: 'DE', name: 'Alemanha', dialCode: '+49' },
  { code: 'BR', name: 'Brasil', dialCode: '+55' },
  { code: 'US', name: 'Estados Unidos', dialCode: '+1' },
  { code: 'FR', name: 'França', dialCode: '+33' },
];

export const documentTypes = [
  { code: 'CPF', description: 'CPF', countrySelectable: false, allCountries: false, allowedCountries: ['BR'] },
  { code: 'SSN', description: 'Social Security Number', countrySelectable: false, allCountries: false, allowedCountries: ['US'] },
  { code: 'UE', description: 'Documento União Europeia', countrySelectable: true, allCountries: false, allowedCountries: ['DE', 'FR'] },
  { code: 'PASSPORT', description: 'Passaporte', countrySelectable: true, allCountries: true, allowedCountries: [] },
  { code: 'OTHER', description: 'Outro', countrySelectable: true, allCountries: true, allowedCountries: [] },
];

export const annualCart = {
  id: 'cart-1',
  protocol: 'SLS-cart-1',
  status: 'CART',
  lastStep: 'PLAN_SELECTED',
  startedAt: '2026-09-24T15:00:00Z',
  product: { codeId: 'TS1', name: 'Teste Streaming 1', expirationService: true, trial: false, trialDays: null },
  plans: [
    { recurrenceFrequency: 'MONTH', productValue: 25.9, taxValue: 3.06, grossValue: 28.96, hasDiscount: false,
      discountValue: 0, discountCycles: null, netValue: 28.96, maxInstallments: 1, paymentMethods: ['CREDIT', 'DEBIT', 'PIX'] },
    { recurrenceFrequency: 'ANNUAL', productValue: 268.8, taxValue: 31.71, grossValue: 300.51, hasDiscount: true,
      discountValue: 30, discountCycles: 1, netValue: 270.51, maxInstallments: 12, paymentMethods: ['CREDIT', 'DEBIT', 'PIX', 'WALLET'] },
  ],
  selection: { recurrenceFrequency: 'ANNUAL', paymentMethod: null, walletProvider: null, cardBrand: null, installments: null },
  quote: {
    productValue: 268.8,
    taxes: [{ name: 'CBS', rate: 8.8, value: 23.65 }, { name: 'IBS', rate: 3, value: 8.06 }],
    taxValue: 31.71, grossValue: 300.51, discountValue: 30, discountCycles: 1, netValue: 270.51, chargedValue: 270.51,
    trialDays: null, firstChargeDate: null, recurrenceAnchorDate: '2026-09-24', maxInstallments: 12,
    installmentOptions: Array.from({ length: 12 }, (_, i) => ({
      installments: i + 1, installmentValue: Math.floor((270.51 / (i + 1)) * 100) / 100,
    })),
  },
  checkoutEnabled: false,
};

export async function mockReferenceData(page: Page) {
  await page.route('**/api/v1/reference/countries', (route) => route.fulfill({ json: countries }));
  await page.route('**/api/v1/reference/document-types', (route) => route.fulfill({ json: documentTypes }));
}

/** Simula um assinante logado (token no sessionStorage, como o AuthProvider faz). */
export async function loginAs(page: Page) {
  await page.addInitScript(() => {
    sessionStorage.setItem('sales.accessToken', 'fake-token');
    sessionStorage.setItem('sales.subscriber', JSON.stringify({
      externalId: '1', name: 'Angelo Alvarenga', email: 'angelo@example.com', authorizedFallback: true,
    }));
  });
}
