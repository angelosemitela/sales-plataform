import type { Cart } from '../../api/types.ts';
import { formatBRL, formatPercent } from '../../lib/format.ts';
import { discountDurationText, recurrenceText, trialText } from '../../lib/purchaseTexts.ts';

/**
 * Resumo de valores (regras 2.2.1 a 2.2.4). Todos os NÚMEROS vêm prontos da API
 * (cotação calculada no backend); aqui só formatamos e montamos os textos.
 */
export function PriceSummary({ cart }: { cart: Cart }) {
  const { quote, selection } = cart;
  const frequency = selection.recurrenceFrequency;
  const hasDiscount = quote.discountValue > 0;

  return (
    <div className="flex flex-col gap-3 text-sm" data-testid="price-summary">
      <dl className="grid grid-cols-[1fr_auto] gap-x-4 gap-y-1">
        <dt>Produto</dt>
        <dd className="text-right">{formatBRL(quote.productValue)}</dd>
        {quote.taxes.map((tax) => (
          <div key={tax.name} className="contents text-slate-500">
            <dt>{tax.name} ({formatPercent(tax.rate)})</dt>
            <dd className="text-right">{formatBRL(tax.value)}</dd>
          </div>
        ))}
        <dt className="font-medium">Produto + taxas</dt>
        <dd className="text-right font-medium" data-testid="gross-value">
          {/* 2.2.1: tachado quando há desconto */}
          {hasDiscount ? <s className="text-slate-400">{formatBRL(quote.grossValue)}</s> : formatBRL(quote.grossValue)}
        </dd>
      </dl>

      {hasDiscount && (
        // 2.2.2: valor com desconto + duração do desconto
        <div className="rounded-md bg-emerald-50 px-3 py-2 text-emerald-800" data-testid="discount">
          <p>Desconto de {formatBRL(quote.discountValue)} {discountDurationText(quote.discountCycles, frequency)}</p>
          <p className="text-lg font-semibold">
            {formatBRL(quote.netValue)}{' '}
            <span className="text-sm font-normal">{discountDurationText(quote.discountCycles, frequency)}</span>
          </p>
        </div>
      )}

      {quote.trialDays !== null && quote.firstChargeDate && (
        // 2.2.3
        <p className="rounded-md bg-indigo-50 px-3 py-2 text-indigo-800" data-testid="trial-message">
          {trialText(quote.trialDays, quote.firstChargeDate)}
        </p>
      )}

      {quote.recurrenceAnchorDate && (
        // 2.2.4 (só recorrente e sem trial - a API manda a data só nesses casos)
        <p className="text-slate-600" data-testid="recurrence-message">
          {recurrenceText(frequency, quote.recurrenceAnchorDate)}
        </p>
      )}

      <p className="flex justify-between border-t pt-2 text-base font-semibold">
        <span>Valor a pagar hoje</span>
        <span data-testid="charged-value">{formatBRL(quote.chargedValue)}</span>
      </p>
    </div>
  );
}
