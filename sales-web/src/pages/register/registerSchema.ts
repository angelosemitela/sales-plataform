import { z } from 'zod';
import { isValidCpf } from '../../lib/cpf.ts';
import { EMAIL_REGEX } from '../../lib/email.ts';
import type { RegisterRequest } from '../../api/types.ts';

/**
 * Regras do formulário de cadastro (mesmos campos do "account" do billing +
 * senha). Regras que dependem de OUTROS campos (confirmação de senha, CPF só
 * quando o tipo é CPF, CEP de 8 dígitos só em endereço nacional) ficam no
 * superRefine, que enxerga o objeto inteiro.
 */
export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'Informe o nome').max(120),
    email: z.string().trim().regex(EMAIL_REGEX, 'Informe um e-mail válido (ex: nome@dominio.com)'),
    password: z.string().min(8, 'A senha deve ter pelo menos 8 caracteres').max(72),
    passwordConfirmation: z.string(),
    isAuthorizedFallback: z.boolean(),

    documentType: z.enum(['CPF', 'SSN', 'UE', 'PASSPORT', 'OTHER']),
    documentCountry: z.string().length(2, 'Selecione o país'),
    documentValue: z.string().trim().min(1, 'Informe o documento').max(40),
    documentDescription: z.string().max(120),

    addressType: z.enum(['RESIDENCIAL', 'COMERCIAL', 'OTHER']),
    addressDescription: z.string().trim().min(1, 'Informe a descrição do endereço').max(120),
    international: z.boolean(),
    // Opcional: o checkbox fica DESABILITADO em endereço internacional e um input
    // desabilitado pode chegar sem valor no envio.
    knowsZipCode: z.boolean().optional(),
    zipCode: z.string().trim().min(1, 'Informe o CEP / código postal').max(20),
    addressName: z.string().trim().min(1, 'Informe o endereço').max(200),
    number: z.string().regex(/^\d{0,10}$/, 'Apenas números'),
    complement: z.string().max(120),
    district: z.string().max(120),
    city: z.string().max(120),
    state: z.string().max(60),
    addressCountry: z.string().length(2, 'Selecione o país'),

    phoneCountry: z.string().length(2, 'Selecione o país'),
    phoneNumber: z.string().regex(/^\d{1,20}$/, 'Informe apenas números (até 20)'),
  })
  .superRefine((form, ctx) => {
    if (form.password !== form.passwordConfirmation) {
      ctx.addIssue({ code: 'custom', path: ['passwordConfirmation'], message: 'As senhas não conferem' });
    }
    if (form.documentType === 'CPF' && !isValidCpf(form.documentValue)) {
      ctx.addIssue({ code: 'custom', path: ['documentValue'], message: 'CPF inválido' });
    }
    if (!form.international && !/^\d{8}$/.test(form.zipCode)) {
      ctx.addIssue({ code: 'custom', path: ['zipCode'], message: 'O CEP deve ter 8 dígitos' });
    }
  });

export type RegisterForm = z.infer<typeof registerSchema>;

export const registerDefaults: RegisterForm = {
  name: '',
  email: '',
  password: '',
  passwordConfirmation: '',
  isAuthorizedFallback: true, // regra 1.1.2: padrão marcado
  documentType: 'CPF',
  documentCountry: 'BR',
  documentValue: '',
  documentDescription: '',
  addressType: 'RESIDENCIAL',
  addressDescription: '',
  international: false, // regra 1.1.3.2: padrão desmarcado
  knowsZipCode: true, // regra 1.1.3.2.1: padrão marcado
  zipCode: '',
  addressName: '',
  number: '',
  complement: '',
  district: '',
  city: '',
  state: '',
  addressCountry: 'BR',
  phoneCountry: 'BR',
  phoneNumber: '',
};

const blankToNull = (value: string) => (value.trim() === '' ? null : value.trim());

/** Formulário (plano, pensado para a tela) -> contrato da API (agrupado). */
export function toRegisterRequest(form: RegisterForm): RegisterRequest {
  return {
    name: form.name.trim(),
    email: form.email.trim(),
    password: form.password,
    isAuthorizedFallback: form.isAuthorizedFallback,
    document: {
      type: form.documentType,
      description: blankToNull(form.documentDescription),
      value: form.documentValue.trim(),
      country: form.documentCountry,
    },
    address: {
      type: form.addressType,
      description: form.addressDescription.trim(),
      international: form.international,
      zipCode: form.zipCode.trim(),
      addressName: form.addressName.trim(),
      number: blankToNull(form.number),
      complement: blankToNull(form.complement),
      district: blankToNull(form.district),
      city: blankToNull(form.city),
      state: blankToNull(form.state),
      country: form.international ? form.addressCountry : 'BR',
    },
    phone: { country: form.phoneCountry, number: form.phoneNumber },
  };
}
