import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../api/client.ts';
import { salesApi } from '../../api/endpoints.ts';
import { errorMessage } from '../../api/messages.ts';
import type { Cart, CartSelection, PaymentMethod, RecurrenceFrequency, WalletProvider } from '../../api/types.ts';
import { Alert, Button, Card, Field, Select } from '../../components/ui.tsx';
import { detectBrand, isAcceptedBrand } from '../../lib/cardBrand.ts';
import { formatBRL } from '../../lib/format.ts';
import { FREQUENCY_LABELS, METHOD_LABELS } from '../../lib/purchaseTexts.ts';
import { CardFields, EMPTY_CARD, type CardData } from './CardFields.tsx';
import { PriceSummary } from './PriceSummary.tsx';

const WALLETS: { value: WalletProvider; label: string }[] = [
  { value: 'PICPAY', label: 'PicPay' },
  { value: 'MERCADO_PAGO', label: 'Mercado Pago' },
];

const isCard = (method: PaymentMethod | null) => method === 'CREDIT' || method === 'DEBIT';

/**
 * Tela de compra (regra 2). Cada escolha do assinante é enviada à API
 * (PUT /selection), que devolve a cotação recalculada - preço e parcelas nunca
 * são calculados aqui. De quebra, o backend registra até onde o assinante chegou
 * (base do relatório de carrinho abandonado).
 */
export function PurchasePage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [card, setCard] = useState<CardData>(EMPTY_CARD);
  // Seleção "otimista": o que o assinante acabou de escolher, enquanto a API não confirma.
  const [optimistic, setOptimistic] = useState<CartSelection | null>(null);

  const cartQuery = useQuery({ queryKey: ['cart', id], queryFn: () => salesApi.getCart(id), staleTime: 0 });
  const cart = cartQuery.data;

  const update = useMutation({
    mutationFn: (selection: CartSelection) => salesApi.updateSelection(id, selection),
    onSuccess: (updated) => queryClient.setQueryData(['cart', id], updated),
  });

  const restart = useMutation({
    mutationFn: (current: Cart) => salesApi.startCart(current.product.codeId, current.selection.recurrenceFrequency),
    onSuccess: (newCart) => {
      update.reset();
      setOptimistic(null);
      setCard(EMPTY_CARD);
      navigate(`/compra/${newCart.id}`, { replace: true });
    },
  });

  // Bandeira detectada no número digitado -> enviada à API (só a bandeira, nunca o número).
  const detected = detectBrand(card.number);
  const acceptedBrand = isAcceptedBrand(detected) ? detected : null;
  useEffect(() => {
    if (!cart || cart.status !== 'CART' || !isCard(cart.selection.paymentMethod) || update.isPending) return;
    if (cart.selection.cardBrand !== acceptedBrand) {
      update.mutate({ ...cart.selection, cardBrand: acceptedBrand });
    }
    // Dependências mínimas DE PROPÓSITO: só reage quando a bandeira ou o método mudam
    // (reagir a cada nova resposta da API poderia gerar chamadas em cascata).
  }, [acceptedBrand, cart?.selection.paymentMethod]);

  if (cartQuery.isPending) return <p>Carregando...</p>;
  if (cartQuery.isError || !cart) return <Alert>{errorMessage(cartQuery.error)}</Alert>;

  const expired = cart.status !== 'CART'
    || (update.error instanceof ApiError && update.error.code === 'CART_NOT_OPEN');
  // UI OTIMISTA: a tela mostra na hora o que o assinante escolheu e só depois a API confirma.
  //
  // Por que um useState, e não o `update.variables` do TanStack Query (1ª tentativa)?
  // O radio é CONTROLADO (checked vem do estado). Logo depois do onChange, o React
  // redesenha e, se o estado ainda não mudou, DESMARCA o radio de novo. O estado da
  // mutation (isPending/variables) é notificado pelo TanStack num tick posterior - tarde
  // demais: o Playwright conferia o radio nesse meio-tempo e o via desmarcado. Um
  // setState chamado DENTRO do handler entra no mesmo render do evento, sem esse intervalo.
  const selection: CartSelection = optimistic ?? cart.selection;
  const plan = cart.plans.find((p) => p.recurrenceFrequency === selection.recurrenceFrequency);
  const methods = plan?.paymentMethods ?? [];

  const send = (patch: Partial<CartSelection>) => {
    const next = { ...selection, ...patch };
    setOptimistic(next);
    // Sucesso ou erro, a seleção confirmada volta a ser a da API (cache do carrinho).
    // Se a API recusar, a tela volta sozinha para a última seleção válida.
    update.mutate(next, { onSettled: () => setOptimistic(null) });
  };

  const changePlan = (frequency: RecurrenceFrequency) => {
    const nextPlan = cart.plans.find((p) => p.recurrenceFrequency === frequency);
    // Mantém o método se o novo plano também o aceitar; parcelas voltam para o padrão (máximo).
    const method = selection.paymentMethod && nextPlan?.paymentMethods.includes(selection.paymentMethod)
      ? selection.paymentMethod : null;
    send({
      recurrenceFrequency: frequency,
      paymentMethod: method,
      walletProvider: method === 'WALLET' ? selection.walletProvider : null,
      cardBrand: isCard(method) ? selection.cardBrand : null,
      installments: null,
    });
  };

  const changeMethod = (method: PaymentMethod) =>
    send({ paymentMethod: method, walletProvider: null, cardBrand: isCard(method) ? acceptedBrand : null, installments: null });

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-semibold" data-testid="product-name">{cart.product.name}</h1>

        {expired && (
          <Alert>
            <div className="flex items-center justify-between gap-4">
              <span>Seu carrinho expirou por inatividade.</span>
              <Button variant="secondary" onClick={() => restart.mutate(cart)} disabled={restart.isPending}>
                Recomeçar
              </Button>
            </div>
          </Alert>
        )}
        {update.isError && !expired && <Alert>{errorMessage(update.error)}</Alert>}

        {cart.plans.length > 1 && (
          <Card title="Plano">
            <div className="flex flex-wrap gap-4" role="radiogroup" aria-label="Plano">
              {cart.plans.map((p) => (
                <label key={p.recurrenceFrequency} className="flex items-center gap-2 text-sm">
                  <input type="radio" name="plan" value={p.recurrenceFrequency} disabled={expired}
                         checked={selection.recurrenceFrequency === p.recurrenceFrequency}
                         onChange={() => changePlan(p.recurrenceFrequency)} />
                  {FREQUENCY_LABELS[p.recurrenceFrequency]}
                </label>
              ))}
            </div>
          </Card>
        )}

        <Card title="Pagamento">
          <div className="flex flex-col gap-5">
            {/* 2.3.1: métodos liberados no plano, seleção exclusiva */}
            <div className="flex flex-wrap gap-4" role="radiogroup" aria-label="Método de pagamento">
              {methods.map((method) => (
                <label key={method} className="flex items-center gap-2 text-sm">
                  <input type="radio" name="paymentMethod" value={method} disabled={expired}
                         checked={selection.paymentMethod === method} onChange={() => changeMethod(method)} />
                  {METHOD_LABELS[method]}
                </label>
              ))}
            </div>

            {selection.paymentMethod && (
              // 2.3.2: combo de parcelas; padrão = máximo liberado (plano x método x parcela mínima de R$ 5)
              <Field label="Parcelas" htmlFor="installments">
                <Select
                  id="installments"
                  value={selection.installments ?? cart.quote.maxInstallments}
                  disabled={expired || cart.quote.installmentOptions.length <= 1}
                  onChange={(e) => send({ installments: Number(e.target.value) })}
                >
                  {cart.quote.installmentOptions.map((option) => (
                    <option key={option.installments} value={option.installments}>
                      {option.installments}x de {formatBRL(option.installmentValue)}
                      {option.installments === 1 ? ' (à vista)' : ''}
                    </option>
                  ))}
                </Select>
              </Field>
            )}

            {isCard(selection.paymentMethod) && <CardFields card={card} onChange={setCard} />}

            {selection.paymentMethod === 'WALLET' && (
              <div className="flex gap-6" role="radiogroup" aria-label="Carteira">
                {WALLETS.map((wallet) => (
                  <label key={wallet.value} className="flex items-center gap-2 text-sm">
                    <input type="radio" name="wallet" value={wallet.value} disabled={expired}
                           checked={selection.walletProvider === wallet.value}
                           onChange={() => send({ walletProvider: wallet.value })} />
                    {wallet.label}
                  </label>
                ))}
              </div>
            )}

            {selection.paymentMethod === 'PIX' && (
              <p className="text-sm text-slate-600">O código PIX será gerado ao finalizar a compra.</p>
            )}
          </div>
        </Card>
      </div>

      <aside className="flex flex-col gap-4">
        <Card title="Resumo">
          <PriceSummary cart={cart} />
        </Card>
        {/* 2.4: botão "Comprar" desabilitado nesta entrega (toggle CHECKOUT_ENABLED no backend). */}
        <Button disabled={!cart.checkoutEnabled || expired} data-testid="buy-button" className="w-full py-3">
          Comprar
        </Button>
        {!cart.checkoutEnabled && (
          <p className="text-center text-xs text-slate-500">A finalização da compra estará disponível em breve.</p>
        )}
      </aside>
    </div>
  );
}
