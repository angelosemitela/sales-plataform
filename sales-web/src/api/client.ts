import type { ProblemDetail } from './types.ts';

/** Erro de API com o "code" estável do backend (é nele que a tela se baseia, não no texto). */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: { field: string; message: string }[];

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? 'Erro inesperado');
    this.status = problem.status;
    this.code = problem.code ?? `HTTP_${problem.status}`;
    this.fieldErrors = problem.errors ?? [];
  }
}

const TOKEN_KEY = 'sales.accessToken';

/**
 * Token guardado no sessionStorage: sobrevive a um F5, mas some ao fechar a aba.
 * Trade-off didático: qualquer script da página consegue ler o sessionStorage (risco
 * em caso de XSS). A alternativa mais segura é o padrão BFF com cookie httpOnly -
 * ver README.
 */
export const tokenStore = {
  get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
  set: (token: string) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
};

let onUnauthorized: () => void = () => {};

/** O AuthProvider registra aqui o que fazer quando a API responder 401 (token vencido). */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  const token = tokenStore.get();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`/api/v1${path}`, { ...init, headers });
  if (response.status === 401 && token) {
    onUnauthorized();
  }
  if (!response.ok) {
    let problem: ProblemDetail = { status: response.status };
    try {
      problem = { ...(await response.json()), status: response.status };
    } catch {
      // corpo vazio ou não-JSON: mantém só o status
    }
    throw new ApiError(problem);
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}
