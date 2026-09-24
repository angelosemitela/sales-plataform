import { ApiError } from './client.ts';

/** Tradução dos códigos de erro da API para mensagens da interface. */
const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: 'E-mail ou senha inválidos.',
  EMAIL_ALREADY_REGISTERED: 'Este e-mail já está cadastrado.',
  DOCUMENT_ALREADY_REGISTERED: 'Este documento já está cadastrado.',
  INVALID_CPF: 'CPF inválido.',
  DOCUMENT_COUNTRY_NOT_ALLOWED: 'País não permitido para este tipo de documento.',
  INVALID_ZIP_CODE: 'CEP inválido.',
  ZIP_CODE_NOT_FOUND: 'CEP não encontrado. Confira o número ou preencha o endereço manualmente.',
  ZIP_CODE_SERVICE_UNAVAILABLE: 'Consulta de CEP indisponível no momento. Preencha o endereço manualmente.',
  PRODUCT_ALREADY_PURCHASED: 'Você já possui este produto. Ele permite apenas uma compra por assinante.',
  PAYMENT_METHOD_NOT_ACCEPTED: 'Método de pagamento não aceito para este plano.',
  INSTALLMENTS_NOT_ALLOWED: 'Quantidade de parcelas não permitida.',
  CART_NOT_OPEN: 'Seu carrinho expirou por inatividade.',
  CHECKOUT_DISABLED: 'A finalização de compras ainda não está disponível.',
  VALIDATION_ERROR: 'Confira os campos destacados.',
  DATA_CONFLICT: 'Não foi possível concluir agora. Tente novamente.',
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ?? error.message;
  }
  return 'Não foi possível falar com o servidor. Tente novamente.';
}
