/**
 * Detecção de bandeira pelo início do número (BIN/IIN).
 *
 * A ORDEM importa: vários BINs da Elo começam com 4 ou 5 (ex: 438935, 504175),
 * então a Elo é testada antes de Visa/MasterCard. Por isso a detecção é
 * "incremental": com poucos dígitos, "4" parece Visa; ao chegar em "438935",
 * vira Elo.
 *
 * Tabela de BINs baseada nas faixas públicas mais usadas - em produção, o
 * gateway de pagamento é a fonte oficial (e costuma devolver a bandeira na
 * tokenização).
 */
export type AcceptedBrand = 'AMEX' | 'ELO' | 'MASTERCARD' | 'VISA';
export type DetectedBrand = AcceptedBrand | 'HIPERCARD' | 'DINERS' | 'DISCOVER' | 'JCB';

export const ACCEPTED_BRANDS: readonly AcceptedBrand[] = ['AMEX', 'ELO', 'MASTERCARD', 'VISA'];

export const BRAND_LABELS: Record<DetectedBrand, string> = {
  AMEX: 'American Express',
  ELO: 'Elo',
  MASTERCARD: 'MasterCard',
  VISA: 'Visa',
  HIPERCARD: 'Hipercard',
  DINERS: 'Diners Club',
  DISCOVER: 'Discover',
  JCB: 'JCB',
};

const ELO_RANGES: ReadonlyArray<readonly [number, number]> = [
  [401178, 401179], [431274, 431274], [438935, 438935], [451416, 451416], [457393, 457393],
  [457631, 457632], [504175, 504175], [506699, 506778], [509000, 509999], [627780, 627780],
  [636297, 636297], [636368, 636368], [650031, 650033], [650035, 650051], [650405, 650439],
  [650485, 650538], [650541, 650598], [650700, 650718], [650720, 650727], [650901, 650978],
  [651652, 651679], [655000, 655019], [655021, 655058],
];

function inRange(prefix: number, from: number, to: number): boolean {
  return prefix >= from && prefix <= to;
}

export function detectBrand(cardNumber: string): DetectedBrand | null {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 1) {
    return null;
  }
  if (digits.length >= 6) {
    const bin = Number(digits.slice(0, 6));
    if (ELO_RANGES.some(([from, to]) => inRange(bin, from, to))) {
      return 'ELO';
    }
    if (bin === 606282) {
      return 'HIPERCARD';
    }
  }
  const p2 = Number(digits.slice(0, 2));
  const p3 = Number(digits.slice(0, 3));
  const p4 = Number(digits.slice(0, 4));

  if (digits.length >= 4 && p4 === 3841) return 'HIPERCARD';
  if (p2 === 34 || p2 === 37) return 'AMEX';
  if (digits.length >= 4 && inRange(p4, 3528, 3589)) return 'JCB';
  if ((digits.length >= 3 && inRange(p3, 300, 305)) || p2 === 36 || p2 === 38) return 'DINERS';
  if ((digits.length >= 4 && p4 === 6011) || (digits.length >= 3 && inRange(p3, 644, 649)) || p2 === 65) return 'DISCOVER';
  if (inRange(p2, 51, 55) || (digits.length >= 4 && inRange(p4, 2221, 2720))) return 'MASTERCARD';
  if (digits[0] === '4') return 'VISA';
  return null;
}

export function isAcceptedBrand(brand: DetectedBrand | null): brand is AcceptedBrand {
  return brand !== null && (ACCEPTED_BRANDS as readonly string[]).includes(brand);
}

/** Algoritmo de Luhn (módulo 10): pega a maioria dos erros de digitação. */
export function passesLuhn(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 13 || digits.length > 19) {
    return false;
  }
  let sum = 0;
  let double = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = Number(digits[i]);
    if (double) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    sum += d;
    double = !double;
  }
  return sum % 10 === 0;
}

/** Formata em blocos de 4 (Amex: 4-6-5), só para exibição. */
export function formatCardNumber(cardNumber: string): string {
  const digits = cardNumber.replace(/\D/g, '').slice(0, 19);
  if (detectBrand(digits) === 'AMEX') {
    return [digits.slice(0, 4), digits.slice(4, 10), digits.slice(10, 15)].filter(Boolean).join(' ');
  }
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
}

/** CVV: 4 dígitos na Amex, 3 nas demais. */
export function cvvLength(brand: DetectedBrand | null): number {
  return brand === 'AMEX' ? 4 : 3;
}
