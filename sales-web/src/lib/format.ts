const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** A API devolve valores como número (BigDecimal serializado); aqui só formatamos. */
export function formatBRL(value: number | string): string {
  return BRL.format(Number(value));
}

/**
 * Datas "LocalDate" chegam como "2026-10-02". Formatamos manualmente, SEM `new Date(...)`:
 * `new Date("2026-10-02")` é interpretado em UTC e, no fuso -03:00, viraria 01/10.
 */
export function formatIsoDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}

export function dayAndMonth(isoDate: string): { day: number; month: number } {
  const [, month, day] = isoDate.split('-');
  return { day: Number(day), month: Number(month) };
}

export function formatPercent(rate: number | string): string {
  return `${Number(rate).toLocaleString('pt-BR', { maximumFractionDigits: 4 })}%`;
}
