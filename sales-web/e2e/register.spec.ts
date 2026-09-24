import { expect, test } from '@playwright/test';
import { mockReferenceData } from './mocks.ts';

test.describe('Cadastro do assinante', () => {
  test.beforeEach(async ({ page }) => {
    await mockReferenceData(page);
    await page.goto('/cadastro');
  });

  test('autorização de cobrança alternativa vem marcada por padrão', async ({ page }) => {
    await expect(page.getByLabel('Autorizo ser cobrado em métodos alternativos em meus cartões múltiplos')).toBeChecked();
    await expect(page.getByLabel('Endereço internacional?')).not.toBeChecked();
    await expect(page.getByLabel('Sei meu CEP')).toBeChecked();
  });

  test('CPF aceita só números e valida os dígitos verificadores', async ({ page }) => {
    const cpf = page.locator('#documentValue');
    await cpf.fill('529.982.247-24');
    await expect(cpf).toHaveValue('52998224724');
    await cpf.blur();
    await expect(page.getByTestId('error-documentValue')).toHaveText('CPF inválido');

    await cpf.fill('52998224725');
    await cpf.blur();
    await expect(page.getByTestId('error-documentValue')).toHaveCount(0);
  });

  test('país do documento: fixo no CPF, seleção restrita na UE', async ({ page }) => {
    await expect(page.locator('#documentCountry')).toHaveValue('Brasil');
    await page.locator('#documentType').selectOption('UE');
    const options = page.locator('#documentCountry option');
    await expect(options).toHaveText(['Alemanha', 'França']);
  });

  test('e-mail inválido é apontado', async ({ page }) => {
    await page.locator('#email').fill('angelo@example');
    await page.locator('#email').blur();
    await expect(page.getByTestId('error-email')).toBeVisible();
  });

  test('CEP preenche o endereço e libera só os campos que vieram vazios', async ({ page }) => {
    await page.route('**/api/v1/addresses/zip-codes/24220000', (route) => route.fulfill({
      json: { zipCode: '24220000', addressName: 'Rua Uno', district: null, city: 'Niterói', state: 'RJ', country: 'BR' },
    }));
    await expect(page.locator('#addressName')).toHaveAttribute('readonly', '');
    await page.locator('#zipCode').fill('24220000');

    await expect(page.locator('#addressName')).toHaveValue('Rua Uno');
    await expect(page.locator('#addressName')).toHaveAttribute('readonly', '');
    await expect(page.locator('#district')).not.toHaveAttribute('readonly', ''); // não veio na consulta
    await expect(page.locator('#addressCountry')).toHaveValue('Brasil');
  });

  test('endereço internacional libera todos os campos e desabilita "Sei meu CEP"', async ({ page }) => {
    await page.getByLabel('Endereço internacional?').check();
    await expect(page.getByLabel('Sei meu CEP')).toBeDisabled();
    await expect(page.locator('#addressName')).not.toHaveAttribute('readonly', '');
    await page.locator('#addressCountry').selectOption('FR');
  });
});
