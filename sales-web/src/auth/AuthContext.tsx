import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { setUnauthorizedHandler, tokenStore } from '../api/client.ts';
import type { AuthResponse, Subscriber } from '../api/types.ts';

interface AuthState {
  subscriber: Subscriber | null;
  isAuthenticated: boolean;
  signIn: (auth: AuthResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);
const SUBSCRIBER_KEY = 'sales.subscriber';

function readStoredSubscriber(): Subscriber | null {
  const raw = sessionStorage.getItem(SUBSCRIBER_KEY);
  return raw && tokenStore.get() ? (JSON.parse(raw) as Subscriber) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [subscriber, setSubscriber] = useState<Subscriber | null>(readStoredSubscriber);

  const signIn = useCallback((auth: AuthResponse) => {
    tokenStore.set(auth.accessToken);
    sessionStorage.setItem(SUBSCRIBER_KEY, JSON.stringify(auth.subscriber));
    setSubscriber(auth.subscriber);
  }, []);

  const signOut = useCallback(() => {
    tokenStore.clear();
    sessionStorage.removeItem(SUBSCRIBER_KEY);
    setSubscriber(null);
  }, []);

  // Token vencido (API respondeu 401): desloga e as rotas protegidas mandam para o login.
  useEffect(() => setUnauthorizedHandler(signOut), [signOut]);

  const value = useMemo(
    () => ({ subscriber, isAuthenticated: subscriber !== null, signIn, signOut }),
    [subscriber, signIn, signOut],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}
