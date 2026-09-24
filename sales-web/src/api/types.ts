/**
 * Tipos do contrato da API do sales-service.
 *
 * Escritos à mão aqui para fins didáticos. Evolução natural: GERAR estes tipos a
 * partir do OpenAPI do backend (/v3/api-docs) com orval ou openapi-typescript - aí
 * front e back nunca divergem.
 */
import type { AcceptedBrand } from '../lib/cardBrand.ts';
import type { PaymentMethod, RecurrenceFrequency } from '../lib/purchaseTexts.ts';

export type { PaymentMethod, RecurrenceFrequency };
export type WalletProvider = 'PICPAY' | 'MERCADO_PAGO';
export type AddressType = 'RESIDENCIAL' | 'COMERCIAL' | 'OTHER';
export type PurchaseStatus = 'CART' | 'ABANDONED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface Country {
  code: string;
  name: string;
  dialCode: string;
}

export interface DocumentType {
  code: 'CPF' | 'SSN' | 'UE' | 'PASSPORT' | 'OTHER';
  description: string;
  countrySelectable: boolean;
  allCountries: boolean;
  allowedCountries: string[];
}

export interface ZipCodeAddress {
  zipCode: string;
  addressName: string | null;
  district: string | null;
  city: string | null;
  state: string | null;
  country: string;
}

export interface Subscriber {
  externalId: string;
  name: string;
  email: string;
  authorizedFallback: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  subscriber: Subscriber;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  isAuthorizedFallback: boolean;
  document: { type: string; description: string | null; value: string; country: string };
  address: {
    type: AddressType;
    description: string;
    international: boolean;
    zipCode: string;
    addressName: string;
    number: string | null;
    complement: string | null;
    district: string | null;
    city: string | null;
    state: string | null;
    country: string;
  };
  phone: { country: string; number: string };
}

export interface CatalogPlan {
  recurrenceFrequency: RecurrenceFrequency;
  productValue: number;
  taxValue: number;
  grossValue: number;
  hasDiscount: boolean;
  discountValue: number;
  discountCycles: number | null;
  netValue: number;
  maxInstallments: number;
  paymentMethods: PaymentMethod[];
}

export interface CatalogProduct {
  codeId: string;
  name: string;
  expirationService: boolean;
  exclusivePurchase: boolean;
  trial: boolean;
  trialDays: number | null;
  taxModel: string;
  plans: CatalogPlan[];
}

export interface TaxLine {
  name: string;
  rate: number;
  value: number;
}

export interface InstallmentOption {
  installments: number;
  installmentValue: number;
}

export interface PriceQuote {
  productValue: number;
  taxes: TaxLine[];
  taxValue: number;
  grossValue: number;
  discountValue: number;
  discountCycles: number | null;
  netValue: number;
  chargedValue: number;
  trialDays: number | null;
  firstChargeDate: string | null;
  recurrenceAnchorDate: string | null;
  maxInstallments: number;
  installmentOptions: InstallmentOption[];
}

export interface CartSelection {
  recurrenceFrequency: RecurrenceFrequency;
  paymentMethod: PaymentMethod | null;
  walletProvider: WalletProvider | null;
  cardBrand: AcceptedBrand | null;
  installments: number | null;
}

export interface Cart {
  id: string;
  protocol: string;
  status: PurchaseStatus;
  lastStep: string;
  startedAt: string;
  product: { codeId: string; name: string; expirationService: boolean; trial: boolean; trialDays: number | null };
  plans: CatalogPlan[];
  selection: CartSelection;
  quote: PriceQuote;
  checkoutEnabled: boolean;
}

/** Corpo de erro no padrão Problem Details (RFC 9457) devolvido pela API. */
export interface ProblemDetail {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
  errors?: { field: string; message: string }[];
}
