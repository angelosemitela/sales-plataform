/** Mesma regra do backend (EmailRules.REGEX): algo@dominio.tld, sem espaços. */
export const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_REGEX.test(email.trim());
}
