import { expect, test } from '@playwright/test';
import { annualCart, loginAs } from './mocks.ts';

test.describe('Tela de compra', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await page.route('**/api/v1/purchases/cart-1', (route) => route.fulfill({ json: annualCart }));
    // O PUT devolve o carrinho com a seleção enviada (eco), como o backend faria.
    await page.route('**/api/v1/purchases/cart-1/selection', async (route) => {
      const selection = route.request().postDataJSON();
      await route.fulfill({ json: { ...annualCart, selection: { ...selection, installments: selection.installments ?? 12 } } });
    });
    await page.goto('/compra/cart-1');
  });

  test('exibe desconto com duração, recorrência e botão Comprar desabilitado', async ({ page }) => {
    await expect(page.getByTestId('discount')).toContainText('por 1 ano');
    await expect(page.getByTestId('recurrence-message')).toHaveText('A recorrência será realizada todo dia 24/09.');
    await expect(page.getByTestId('buy-button')).toBeDisabled();
  });

  test('cartão de crédito: parcelas no máximo por padrão e bandeira não aceita é recusada', async ({ page }) => {
    // click() + expect().toBeChecked() em vez de check(): o check() confere o estado UMA vez,
    // logo após o clique; a asserção "web-first" (expect) tenta de novo por até 5s - o jeito
    // recomendado pelo Playwright para telas que dependem de resposta da API.
    const credit = page.getByLabel('Cartão de crédito');
    await credit.click();
    await expect(credit).toBeChecked();
    await expect(page.locator('#installments')).toHaveValue('12');

    await page.locator('#cardNumber').fill('6011111111111117'); // Discover
    await expect(page.getByTestId('error-cardNumber')).toContainText('não é aceito');

    await page.locator('#cardNumber').fill('4111111111111111');
    await expect(page.getByTestId('error-cardNumber')).toHaveCount(0);
    await expect(page.getByText('Bandeira: Visa')).toBeVisible();
  });

  test('carteira digital oferece PicPay e Mercado Pago', async ({ page }) => {
    const wallet = page.getByLabel('Carteira digital');
    await wallet.click();
    await expect(wallet).toBeChecked();
    await expect(page.getByLabel('PicPay')).toBeVisible();
    await expect(page.getByLabel('Mercado Pago')).toBeVisible();
  });
});
