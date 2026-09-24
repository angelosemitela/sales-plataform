import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { Cart } from '../../api/types.ts';
import { PriceSummary } from './PriceSummary.tsx';
import { cardErrors, EMPTY_CARD } from './CardFields.tsx';

const annualCart: Cart = {
  id: 'c1',
  protocol: 'SLS-c1',
  status: 'CART',
  lastStep: 'PLAN_SELECTED',
  startedAt: '2026-09-24T15:00:00Z',
  product: { codeId: 'TS1', name: 'Teste Streaming 1', expirationService: true, trial: false, trialDays: null },
  plans: [],
  selection: { recurrenceFrequency: 'ANNUAL', paymentMethod: null, walletProvider: null, cardBrand: null, installments: null },
  quote: {
    productValue: 268.8,
    taxes: [{ name: 'CBS', rate: 8.8, value: 23.65 }, { name: 'IBS', rate: 3, value: 8.06 }],
    taxValue: 31.71,
    grossValue: 300.51,
    discountValue: 30,
    discountCycles: 1,
    netValue: 270.51,
    chargedValue: 270.51,
    trialDays: null,
    firstChargeDate: null,
    recurrenceAnchorDate: '2026-09-24',
    maxInstallments: 12,
    installmentOptions: [],
  },
  checkoutEnabled: false,
};

describe('PriceSummary', () => {
  it('mostra o valor cheio tachado, o desconto com duração e o dia da recorrência', () => {
    render(<PriceSummary cart={annualCart} />);

    expect(screen.getByTestId('gross-value').querySelector('s')).toHaveTextContent('300,51');
    expect(screen.getByTestId('discount')).toHaveTextContent('por 1 ano');
    expect(screen.getByTestId('discount')).toHaveTextContent('270,51');
    expect(screen.getByTestId('recurrence-message')).toHaveTextContent('todo dia 24/09');
    expect(screen.queryByTestId('trial-message')).not.toBeInTheDocument();
  });

  it('mostra a mensagem de trial e nada a pagar hoje', () => {
    render(<PriceSummary cart={{
      ...annualCart,
      quote: { ...annualCart.quote, discountValue: 0, chargedValue: 0, trialDays: 7, firstChargeDate: '2026-10-02', recurrenceAnchorDate: null },
    }} />);

    expect(screen.getByTestId('trial-message')).toHaveTextContent('Você tem 7 dias grátis. Será cobrado somente em 02/10/2026.');
    expect(screen.getByTestId('charged-value')).toHaveTextContent('0,00');
  });
});

describe('cardErrors', () => {
  const today = new Date(2026, 8, 24);
  it('bandeira não aceita exige outro cartão', () => {
    expect(cardErrors({ ...EMPTY_CARD, number: '6011111111111117' }, today).number).toContain('não é aceito');
  });
  it('cartão válido não tem erros', () => {
    expect(cardErrors({ number: '4111111111111111', expiration: '12/30', holderName: 'A', cvv: '123' }, today)).toEqual({});
  });
  it('CVV da Amex tem 4 dígitos', () => {
    expect(cardErrors({ ...EMPTY_CARD, number: '378282246310005', cvv: '123' }, today).cvv).toContain('4 dígitos');
  });
});
