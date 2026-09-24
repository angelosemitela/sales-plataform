/**
 * Validação LOCAL de CPF (mesmo algoritmo do backend - CpfValidator.java).
 * Dá feedback imediato na tela; o backend valida de novo, porque o front
 * nunca é confiável (qualquer um pode chamar a API direto).
 */
export function isValidCpf(cpf: string): boolean {
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) {
    return false;
  }
  return checkDigit(cpf, 9) === Number(cpf[9]) && checkDigit(cpf, 10) === Number(cpf[10]);
}

function checkDigit(cpf: string, length: number): number {
  let sum = 0;
  for (let i = 0; i < length; i++) {
    sum += Number(cpf[i]) * (length + 1 - i);
  }
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

/** Remove tudo que não é dígito (usado nos campos "só números"). */
export function onlyDigits(value: string, maxLength?: number): string {
  const digits = value.replace(/\D/g, '');
  return maxLength === undefined ? digits : digits.slice(0, maxLength);
}
