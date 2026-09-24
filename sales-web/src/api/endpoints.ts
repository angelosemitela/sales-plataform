import { api } from './client.ts';
import type {
  AuthResponse,
  Cart,
  CartSelection,
  CatalogProduct,
  Country,
  DocumentType,
  RecurrenceFrequency,
  RegisterRequest,
  Subscriber,
  ZipCodeAddress,
} from './types.ts';

/** Uma função por endpoint: as telas nunca montam URL nem fazem fetch direto. */
export const salesApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  register: (request: RegisterRequest) =>
    api<AuthResponse>('/subscribers', { method: 'POST', body: JSON.stringify(request) }),

  me: () => api<Subscriber>('/subscribers/me'),

  countries: () => api<Country[]>('/reference/countries'),

  documentTypes: () => api<DocumentType[]>('/reference/document-types'),

  zipCode: (zipCode: string) => api<ZipCodeAddress>(`/addresses/zip-codes/${zipCode}`),

  catalog: () => api<CatalogProduct[]>('/catalog'),

  features: () => api<{ checkoutEnabled: boolean }>('/features'),

  startCart: (productCode: string, recurrenceFrequency?: RecurrenceFrequency) =>
    api<Cart>('/purchases', { method: 'POST', body: JSON.stringify({ productCode, recurrenceFrequency }) }),

  getCart: (id: string) => api<Cart>(`/purchases/${id}`),

  updateSelection: (id: string, selection: CartSelection) =>
    api<Cart>(`/purchases/${id}/selection`, { method: 'PUT', body: JSON.stringify(selection) }),
};
