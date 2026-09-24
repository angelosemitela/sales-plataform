/**
 * Validade MM/AA (anos 2000-2099), mesma regra do billing e do backend:
 * mês 01-12 e não vencido (um cartão 09/26 vale durante todo setembro/2026).
 */
export function isValidExpiration(expiration: string, today: Date = new Date()): boolean {
  const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(expiration);
  if (!match) {
    return false;
  }
  const month = Number(match[1]);
  const year = 2000 + Number(match[2]);
  const currentYear = today.getFullYear();
  const currentMonth = today.getMonth() + 1;
  return year > currentYear || (year === currentYear && month >= currentMonth);
}

/** Máscara enquanto digita: "0331" -> "03/31". */
export function maskExpiration(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
}
