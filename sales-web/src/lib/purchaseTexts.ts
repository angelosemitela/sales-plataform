import { dayAndMonth, formatIsoDate } from './format.ts';

export type RecurrenceFrequency = 'ONESHOT' | 'MONTH' | 'ANNUAL';
export type PaymentMethod = 'CREDIT' | 'DEBIT' | 'PIX' | 'WALLET';

export const FREQUENCY_LABELS: Record<RecurrenceFrequency, string> = {
  ONESHOT: 'Avulso',
  MONTH: 'Mensal',
  ANNUAL: 'Anual',
};

export const METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT: 'Cartão de crédito',
  DEBIT: 'Cartão de débito',
  PIX: 'PIX',
  WALLET: 'Carteira digital',
};

/** Regra 2.2.2: "por 2 meses", "por 1 ano"... (recurrenceFrequency traduzido). */
export function discountDurationText(cycles: number | null, frequency: RecurrenceFrequency): string {
  if (!cycles || frequency === 'ONESHOT') {
    return '';
  }
  if (frequency === 'MONTH') {
    return cycles === 1 ? 'por 1 mês' : `por ${cycles} meses`;
  }
  return cycles === 1 ? 'por 1 ano' : `por ${cycles} anos`;
}

/** Regra 2.2.3: data da 1ª cobrança já vem calculada pela API (hoje + trialDays + 1). */
export function trialText(trialDays: number, firstChargeDate: string): string {
  const days = trialDays === 1 ? '1 dia grátis' : `${trialDays} dias grátis`;
  return `Você tem ${days}. Será cobrado somente em ${formatIsoDate(firstChargeDate)}.`;
}

/** Regra 2.2.4: dia da recorrência (mensal: "dia 24 de cada mês"; anual: "dia 24/09"). */
export function recurrenceText(frequency: RecurrenceFrequency, anchorDate: string): string {
  const { day, month } = dayAndMonth(anchorDate);
  if (frequency === 'MONTH') {
    const base = `A recorrência será realizada todo dia ${day} de cada mês.`;
    // Mesma regra do billing: mês sem esse dia -> cobra no dia 1º do mês seguinte.
    return day > 28
      ? `${base} Nos meses que não tiverem o dia ${day}, a cobrança acontece no dia 1º do mês seguinte.`
      : base;
  }
  if (frequency === 'ANNUAL') {
    return `A recorrência será realizada todo dia ${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}.`;
  }
  return '';
}
