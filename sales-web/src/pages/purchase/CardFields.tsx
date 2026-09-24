import { Field, Input } from '../../components/ui.tsx';
import {
  BRAND_LABELS,
  cvvLength,
  detectBrand,
  formatCardNumber,
  isAcceptedBrand,
  passesLuhn,
} from '../../lib/cardBrand.ts';
import { isValidExpiration, maskExpiration } from '../../lib/cardExpiration.ts';
import { onlyDigits } from '../../lib/cpf.ts';

export interface CardData {
  number: string;
  expiration: string;
  holderName: string;
  cvv: string;
}

export const EMPTY_CARD: CardData = { number: '', expiration: '', holderName: '', cvv: '' };

/** Erros dos campos do cartão (vazio = ok). Exportado para teste. */
export function cardErrors(card: CardData, today: Date = new Date()): Partial<Record<keyof CardData, string>> {
  const errors: Partial<Record<keyof CardData, string>> = {};
  const digits = onlyDigits(card.number);
  const brand = detectBrand(digits);

  if (digits.length >= 6 && brand === null) {
    errors.number = 'Bandeira não reconhecida. Confira o número do cartão.';
  } else if (brand !== null && !isAcceptedBrand(brand)) {
    // Regra 2.3.2.1: bandeira identificada mas não aceita -> exigir outro cartão.
    errors.number = `Cartão ${BRAND_LABELS[brand]} não é aceito. Use American Express, Elo, MasterCard ou Visa.`;
  } else if (digits.length >= 13 && !passesLuhn(digits) && (digits.length >= 16 || (brand === 'AMEX' && digits.length === 15))) {
    errors.number = 'Número de cartão inválido.';
  }
  if (card.expiration.length === 5 && !isValidExpiration(card.expiration, today)) {
    errors.expiration = 'Validade inválida ou vencida.';
  }
  if (card.cvv.length > 0 && card.cvv.length !== cvvLength(brand)) {
    errors.cvv = `O CVV deve ter ${cvvLength(brand)} dígitos.`;
  }
  return errors;
}

/**
 * Campos do cartão (regra 2.3.2). IMPORTANTE: estes dados ficam SÓ no navegador.
 * Na finalização (fora desta entrega), o número/CVV vão direto ao gateway de
 * pagamento (tokenização) - nosso backend recebe apenas bandeira, 4 finais e tokens.
 */
export function CardFields({ card, onChange }: { card: CardData; onChange: (card: CardData) => void }) {
  const brand = detectBrand(card.number);
  const errors = cardErrors(card);

  return (
    <div className="grid gap-4 sm:grid-cols-2" data-testid="card-fields">
      <div className="sm:col-span-2">
        <Field
          label="Número do cartão"
          htmlFor="cardNumber"
          error={errors.number}
          hint={brand && isAcceptedBrand(brand) ? `Bandeira: ${BRAND_LABELS[brand]}` : undefined}
        >
          <Input
            id="cardNumber"
            inputMode="numeric"
            autoComplete="cc-number"
            value={formatCardNumber(card.number)}
            onChange={(e) => onChange({ ...card, number: onlyDigits(e.target.value, 19) })}
          />
        </Field>
      </div>
      <Field label="Validade (MM/AA)" htmlFor="cardExpiration" error={errors.expiration}>
        <Input
          id="cardExpiration"
          inputMode="numeric"
          autoComplete="cc-exp"
          placeholder="MM/AA"
          value={card.expiration}
          onChange={(e) => onChange({ ...card, expiration: maskExpiration(e.target.value) })}
        />
      </Field>
      <Field label="CVV" htmlFor="cardCvv" error={errors.cvv}>
        <Input
          id="cardCvv"
          inputMode="numeric"
          autoComplete="cc-csc"
          maxLength={cvvLength(brand)}
          value={card.cvv}
          onChange={(e) => onChange({ ...card, cvv: onlyDigits(e.target.value, cvvLength(brand)) })}
        />
      </Field>
      <div className="sm:col-span-2">
        <Field label="Nome impresso no cartão" htmlFor="cardHolder">
          <Input
            id="cardHolder"
            autoComplete="cc-name"
            value={card.holderName}
            onChange={(e) => onChange({ ...card, holderName: e.target.value.toUpperCase() })}
          />
        </Field>
      </div>
    </div>
  );
}
