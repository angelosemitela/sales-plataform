import { describe, expect, it } from 'vitest';
import { isValidCpf, onlyDigits } from '../cpf.ts';
import { detectBrand, formatCardNumber, isAcceptedBrand, passesLuhn } from '../cardBrand.ts';
import { isValidExpiration, maskExpiration } from '../cardExpiration.ts';
import { formatIsoDate } from '../format.ts';
import { discountDurationText, recurrenceText, trialText } from '../purchaseTexts.ts';
import { isValidEmail } from '../email.ts';

describe('CPF (regra 1.1.2.1)', () => {
  it.each(['52998224725', '11144477735', '39053344705'])('aceita %s', (cpf) => {
    expect(isValidCpf(cpf)).toBe(true);
  });
  it.each(['52998224724', '11111111111', '5299822472', '529.982.247-25'])('rejeita %s', (cpf) => {
    expect(isValidCpf(cpf)).toBe(false);
  });
  it('filtra tudo que não é número', () => {
    expect(onlyDigits('529.982-247a25', 11)).toBe('52998224725');
  });
});

describe('e-mail (regra 1.1.1)', () => {
  it('exige domínio com extensão', () => {
    expect(isValidEmail('nome@dominio.com')).toBe(true);
    expect(isValidEmail('nome@dominio')).toBe(false);
    expect(isValidEmail('nome dominio.com')).toBe(false);
  });
});

describe('bandeira do cartão (regra 2.3.2.1)', () => {
  it.each([
    ['4111111111111111', 'VISA'],
    ['5555555555554444', 'MASTERCARD'],
    ['2221000000000009', 'MASTERCARD'],
    ['378282246310005', 'AMEX'],
    ['6362970000457013', 'ELO'],
    ['4389350000000000', 'ELO'], // BIN Elo que começa com 4: Elo é testada antes de Visa
    ['6011111111111117', 'DISCOVER'],
    ['6062825624254001', 'HIPERCARD'],
  ])('%s -> %s', (number, brand) => {
    expect(detectBrand(number)).toBe(brand);
  });

  it('só aceita Amex, Elo, MasterCard e Visa', () => {
    expect(isAcceptedBrand('ELO')).toBe(true);
    expect(isAcceptedBrand('DISCOVER')).toBe(false);
    expect(isAcceptedBrand(null)).toBe(false);
  });

  it('valida Luhn e formata', () => {
    expect(passesLuhn('4111111111111111')).toBe(true);
    expect(passesLuhn('4111111111111112')).toBe(false);
    expect(formatCardNumber('378282246310005')).toBe('3782 822463 10005');
  });
});

describe('validade do cartão', () => {
  const today = new Date(2026, 8, 24);
  it.each([['09/26', true], ['03/31', true], ['08/26', false], ['12/24', false], ['13/29', false]])(
    '%s -> %s', (expiration, expected) => {
      expect(isValidExpiration(expiration as string, today)).toBe(expected);
    });
  it('aplica a máscara MM/AA', () => {
    expect(maskExpiration('0331')).toBe('03/31');
  });
});

describe('textos da tela de compra', () => {
  it('duração do desconto (2.2.2)', () => {
    expect(discountDurationText(1, 'ANNUAL')).toBe('por 1 ano');
    expect(discountDurationText(2, 'MONTH')).toBe('por 2 meses');
    expect(discountDurationText(null, 'MONTH')).toBe('');
  });
  it('trial (2.2.3)', () => {
    expect(trialText(7, '2026-10-02')).toBe('Você tem 7 dias grátis. Será cobrado somente em 02/10/2026.');
  });
  it('recorrência mensal e anual (2.2.4)', () => {
    expect(recurrenceText('MONTH', '2026-09-24')).toBe('A recorrência será realizada todo dia 24 de cada mês.');
    expect(recurrenceText('ANNUAL', '2026-09-24')).toBe('A recorrência será realizada todo dia 24/09.');
    expect(recurrenceText('MONTH', '2026-01-31')).toContain('dia 1º do mês seguinte');
  });
  it('formata datas sem cair no problema de fuso', () => {
    expect(formatIsoDate('2026-10-02')).toBe('02/10/2026');
  });
});
