import { describe, expect, it } from 'vitest';
import { registerDefaults, registerSchema, toRegisterRequest, type RegisterForm } from './registerSchema.ts';

const valid: RegisterForm = {
  ...registerDefaults,
  name: 'Angelo Alvarenga',
  email: 'angelo@example.com',
  password: 'segredo123',
  passwordConfirmation: 'segredo123',
  documentValue: '52998224725',
  addressDescription: 'Casa',
  zipCode: '24220000',
  addressName: 'Rua Uno',
  phoneNumber: '21999999999',
};

function errorsOf(form: RegisterForm): Record<string, string> {
  const result = registerSchema.safeParse(form);
  if (result.success) return {};
  return Object.fromEntries(result.error.issues.map((issue) => [issue.path.join('.'), issue.message]));
}

describe('registerSchema', () => {
  it('aceita um cadastro válido', () => {
    expect(errorsOf(valid)).toEqual({});
  });

  it('valida o formato do e-mail', () => {
    expect(errorsOf({ ...valid, email: 'angelo@example' })).toHaveProperty('email');
  });

  it('valida os dígitos do CPF só quando o tipo é CPF', () => {
    expect(errorsOf({ ...valid, documentValue: '52998224724' })).toHaveProperty('documentValue', 'CPF inválido');
    expect(errorsOf({ ...valid, documentType: 'PASSPORT', documentValue: 'FX123' })).toEqual({});
  });

  it('exige confirmação de senha igual', () => {
    expect(errorsOf({ ...valid, passwordConfirmation: 'outra' })).toHaveProperty('passwordConfirmation');
  });

  it('exige CEP de 8 dígitos só no endereço nacional', () => {
    expect(errorsOf({ ...valid, zipCode: '1234' })).toHaveProperty('zipCode');
    expect(errorsOf({ ...valid, international: true, zipCode: 'SW1A 1AA', addressCountry: 'GB' })).toEqual({});
  });

  it('número do endereço aceita só dígitos, mas pode ficar vazio', () => {
    expect(errorsOf({ ...valid, number: '12A' })).toHaveProperty('number');
    expect(errorsOf({ ...valid, number: '' })).toEqual({});
  });

  it('converte para o contrato da API, forçando BR em endereço nacional', () => {
    const request = toRegisterRequest({ ...valid, addressCountry: 'US', number: '' });
    expect(request.address.country).toBe('BR');
    expect(request.address.number).toBeNull();
    expect(request.isAuthorizedFallback).toBe(true);
  });
});
