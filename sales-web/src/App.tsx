import { Link, Navigate, Route, Routes } from 'react-router';
import { useAuth } from './auth/AuthContext.tsx';
import { RequireAuth } from './auth/RequireAuth.tsx';
import { LoginPage } from './pages/LoginPage.tsx';
import { RegisterPage } from './pages/register/RegisterPage.tsx';
import { CatalogPage } from './pages/CatalogPage.tsx';
import { PurchasePage } from './pages/purchase/PurchasePage.tsx';

export function App() {
  const { subscriber, signOut } = useAuth();
  return (
    <div className="min-h-screen">
      <header className="border-b bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-semibold text-indigo-700">
            Sales
          </Link>
          {subscriber && (
            <div className="flex items-center gap-3 text-sm">
              <span data-testid="logged-user">Olá, {subscriber.name.split(' ')[0]}</span>
              <button type="button" className="text-slate-500 underline" onClick={signOut}>
                Sair
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/cadastro" element={<RegisterPage />} />
          <Route path="/" element={<RequireAuth><CatalogPage /></RequireAuth>} />
          <Route path="/compra/:id" element={<RequireAuth><PurchasePage /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
