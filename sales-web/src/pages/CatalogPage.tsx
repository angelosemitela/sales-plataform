import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { salesApi } from '../api/endpoints.ts';
import { errorMessage } from '../api/messages.ts';
import type { CatalogProduct } from '../api/types.ts';
import { Alert, Button, Card } from '../components/ui.tsx';
import { formatBRL } from '../lib/format.ts';
import { FREQUENCY_LABELS, discountDurationText } from '../lib/purchaseTexts.ts';

/** Vitrine. Regra 2: clicar no produto INICIA a venda (abre o carrinho). */
export function CatalogPage() {
  const navigate = useNavigate();
  const catalog = useQuery({ queryKey: ['catalog'], queryFn: salesApi.catalog });
  const startCart = useMutation({
    mutationFn: (product: CatalogProduct) => salesApi.startCart(product.codeId),
    onSuccess: (cart) => navigate(`/compra/${cart.id}`),
  });

  if (catalog.isPending) return <p>Carregando catálogo...</p>;
  if (catalog.isError) return <Alert>{errorMessage(catalog.error)}</Alert>;

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">Escolha seu produto</h1>
      {startCart.isError && <Alert>{errorMessage(startCart.error)}</Alert>}
      <div className="grid gap-4 md:grid-cols-3">
        {catalog.data.map((product) => (
          <Card key={product.codeId}>
            <div className="flex h-full flex-col gap-3" data-testid={`product-${product.codeId}`}>
              <div>
                <h2 className="text-lg font-semibold">{product.name}</h2>
                {product.trial && <span className="text-xs text-emerald-700">{product.trialDays} dias grátis</span>}
              </div>
              <ul className="flex flex-col gap-2 text-sm">
                {product.plans.map((plan) => (
                  <li key={plan.recurrenceFrequency} className="rounded border px-3 py-2">
                    <span className="font-medium">{FREQUENCY_LABELS[plan.recurrenceFrequency]}: </span>
                    {plan.hasDiscount ? (
                      <>
                        <s className="text-slate-400">{formatBRL(plan.grossValue)}</s>{' '}
                        <strong>{formatBRL(plan.netValue)}</strong>{' '}
                        <span className="text-xs text-slate-500">
                          {discountDurationText(plan.discountCycles, plan.recurrenceFrequency)}
                        </span>
                      </>
                    ) : (
                      <strong>{formatBRL(plan.grossValue)}</strong>
                    )}
                    {plan.maxInstallments > 1 && (
                      <span className="block text-xs text-slate-500">em até {plan.maxInstallments}x</span>
                    )}
                  </li>
                ))}
              </ul>
              <p className="text-xs text-slate-500">Valores com impostos inclusos.</p>
              <Button className="mt-auto" disabled={startCart.isPending} onClick={() => startCart.mutate(product)}>
                Quero este
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
